package pl.newbies.lecture.application

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import graphql.schema.DataFetchingEnvironment
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.common.principal
import pl.newbies.event.application.assertEventWriteAccess
import pl.newbies.event.domain.EventNotFoundException
import pl.newbies.event.infrastructure.repository.EventDAO
import pl.newbies.event.infrastructure.repository.toEvent
import pl.newbies.lecture.application.model.*
import pl.newbies.lecture.domain.LectureInviteNotFoundException
import pl.newbies.lecture.domain.LectureNotFoundException
import pl.newbies.lecture.domain.model.Lecture
import pl.newbies.lecture.domain.service.LectureService
import pl.newbies.lecture.infrastructure.repository.*
import java.util.concurrent.CompletableFuture
import com.expediagroup.graphql.server.operations.Mutation as GraphQLMutation
import com.expediagroup.graphql.server.operations.Query as GraphQLQuery

class LectureSchema(
    private val lectureConverter: LectureConverter,
    private val lectureRateConverter: LectureRateConverter,
    private val lectureService: LectureService,
) {
    inner class Query : GraphQLQuery {
        @GraphQLDescription("Get all event lectures")
        fun lectures(
            filter: LectureFilter,
        ): List<LectureResponse> {
            return lectureService.getLectures(filter)
                .map { lectureConverter.convert(it) }
        }

        @GraphQLDescription("Get single lecture by its id")
        fun lecture(
            id: String
        ): LectureResponse {
            val lecture = transaction { LectureDAO.findById(id)?.toLecture() }
                ?: throw LectureNotFoundException(id)

            return lectureConverter.convert(lecture)
        }
    }

    inner class Mutation : GraphQLMutation {
        @GraphQLDescription("Create lecture with request")
        fun createLecture(eventId: String, request: LectureRequest, env: DataFetchingEnvironment): LectureResponse {
            val principal = env.principal()
            val event = transaction { EventDAO.findById(eventId)?.toEvent() }
                ?: throw EventNotFoundException(eventId)

            principal.assertEventWriteAccess(event)

            val lecture = lectureService.createLecture(request, event, principal.userId)

            return lectureConverter.convert(lecture)
        }

        @GraphQLDescription("Create lecture speaker invite")
        fun createLectureInvite(lectureId: String, request: LectureInviteRequest, env: DataFetchingEnvironment): LectureInviteResponse {
            val principal = env.principal()
            val lecture = transaction { LectureDAO.findById(lectureId)?.toLecture() }
                ?: throw LectureNotFoundException(lectureId)

            principal.assertLectureWriteAccess(lecture)

            val invite = lectureService.createLectureInvite(lecture, request)

            return lectureConverter.convert(invite)
        }

        @GraphQLDescription("Use lecture invitation")
        fun useLectureInvite(inviteId: String, env: DataFetchingEnvironment): Boolean {
            val principal = env.principal()

            val invite = transaction { LectureInviteDAO.findById(inviteId)?.toInvite() }
                ?: throw LectureInviteNotFoundException(inviteId)

            val lectureId = invite.lectureId

            val lecture = transaction { LectureDAO.findById(lectureId)?.toLecture() }
                ?: throw LectureNotFoundException(lectureId)

            lectureService.useLectureInvite(lecture, invite, principal)

            return true
        }

        @GraphQLDescription("Delete lecture invite by id")
        fun deleteLectureInvite(inviteId: String, env: DataFetchingEnvironment): Boolean {
            val principal = env.principal()

            val invite = transaction { LectureInviteDAO.findById(inviteId)?.toInvite() }
                ?: throw LectureInviteNotFoundException(inviteId)

            val lectureId = invite.lectureId

            val lecture = transaction { LectureDAO.findById(lectureId)?.toLecture() }
                ?: throw LectureNotFoundException(lectureId)

            principal.assertLectureWriteAccess(lecture)

            lectureService.deleteLectureInvite(invite)

            return true
        }

        @GraphQLDescription("Replace lecture data with new data (PUT equivalent)")
        fun replaceLecture(id: String, request: LectureRequest, env: DataFetchingEnvironment): LectureResponse {
            val principal = env.principal()
            val lecture = transaction { LectureDAO.findById(id)?.toLecture() }
                ?: throw LectureNotFoundException(id)

            principal.assertLectureWriteAccess(lecture)

            val replacedLecture = lectureService.updateLecture(lecture, request)

            return lectureConverter.convert(replacedLecture)
        }

        @GraphQLDescription("Delete lecture by id")
        fun deleteLecture(id: String, env: DataFetchingEnvironment): Boolean {
            val principal = env.principal()
            val lecture = transaction { LectureDAO.findById(id)?.toLecture() }
                ?: throw LectureNotFoundException(id)

            principal.assertLectureWriteAccess(lecture)

            lectureService.deleteLecture(lecture)

            return true
        }

        @GraphQLDescription("Creates rate lecture")
        fun rateLecture(id: String, request: LectureRateRequest): LectureRateResponse {
            val lecture = transaction { LectureDAO.findById(id)?.toLecture() }
                ?: throw LectureNotFoundException(id)

            val rate = lectureService.rateLecture(lecture, request)

            return lectureRateConverter.convert(rate)
        }
    }

    inner class LectureRatesDataLoader : KotlinDataLoader<String, List<LectureRateResponse>> {
        override val dataLoaderName: String = "LectureRatesDataLoader"

        override fun getDataLoader(): DataLoader<String, List<LectureRateResponse>> =
            DataLoaderFactory.newDataLoader { lectureIds ->
                CompletableFuture.supplyAsync {
                    val rates = transaction {
                        LectureRateDAO.find {
                            LectureRates.lecture inList lectureIds
                        }
                            .orderBy(LectureRates.createDate to SortOrder.DESC)
                            .map { it.toLectureRate() }
                    }

                    val ratesMap = rates.map { lectureRateConverter.convert(it) }
                        .groupBy { it.lectureId }

                    lectureIds.map {
                        ratesMap.getOrDefault(it, emptyList())
                    }
                }
            }
    }

    inner class LectureInvitesDataLoader : KotlinDataLoader<String, List<LectureInviteResponse>> {
        override val dataLoaderName: String = "LectureInvitesDataLoader"

        override fun getDataLoader(): DataLoader<String, List<LectureInviteResponse>> =
            DataLoaderFactory.newDataLoader { lectureIds, env ->
                CompletableFuture.supplyAsync {
                    val principal = env.principal()

                    val invites = transaction {
                        LectureInviteDAO.find {
                            LectureInvites.lecture inList lectureIds
                        }
                            .map { it.toInvite() }
                    }

                    val lectures = transaction {
                        LectureDAO.find {
                            Lectures.id inList lectureIds
                        }.map { it.toLecture() }
                    }

                    val invitesMap = invites.map { lectureConverter.convert(it) }
                        .groupBy { it.lectureId }

                    lectureIds.map {
                        val lecture = lectures.firstOrNull { lecture: Lecture -> lecture.id == it }
                            ?: return@map emptyList()

                        if (!principal.hasLectureWriteAccess(lecture)) {
                            return@map emptyList()
                        }

                        invitesMap.getOrDefault(it, emptyList())
                    }
                }
            }
    }

    inner class LectureRateSummaryDataLoader : KotlinDataLoader<String, LectureResponse.RateSummary> {
        override val dataLoaderName: String = "LectureRateSummaryDataLoader"

        override fun getDataLoader(): DataLoader<String, LectureResponse.RateSummary> =
            DataLoaderFactory.newDataLoader { lectureIds ->
                CompletableFuture.supplyAsync {
                    val rates = transaction {
                        LectureRateDAO.find {
                            LectureRates.lecture inList lectureIds
                        }.map { it.toLectureRate() }
                    }

                    val ratesMap = rates.map { lectureRateConverter.convert(it) }
                        .groupBy { it.lectureId }

                    lectureIds.map {
                        ratesMap[it]?.let {
                            LectureResponse.RateSummary(
                                votesCount = it.size,
                                topicAvg = it.map { it.topicRate }.average(),
                                presentationAvg = it.map { it.presentationRate }.average(),
                            )
                        } ?: LectureResponse.RateSummary(0, 0.0, 0.0)
                    }
                }
            }
    }
}