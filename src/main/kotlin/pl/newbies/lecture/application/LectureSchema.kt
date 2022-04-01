package pl.newbies.lecture.application

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.execution.KotlinDataLoader
import graphql.schema.DataFetchingEnvironment
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.common.pagination
import pl.newbies.common.principal
import pl.newbies.lecture.application.model.LectureRequest
import pl.newbies.lecture.application.model.LectureResponse
import pl.newbies.lecture.application.model.LectureThemeRequest
import pl.newbies.lecture.domain.LectureNotFoundException
import pl.newbies.lecture.domain.service.LectureService
import pl.newbies.lecture.infrastructure.repository.LectureDAO
import pl.newbies.lecture.infrastructure.repository.LectureFollows
import pl.newbies.lecture.infrastructure.repository.Lectures
import pl.newbies.lecture.infrastructure.repository.toLecture
import pl.newbies.user.application.UserConverter
import pl.newbies.user.application.model.UserResponse
import pl.newbies.user.domain.UserNotFoundException
import pl.newbies.user.infrastructure.repository.UserDAO
import pl.newbies.user.infrastructure.repository.toUser
import java.util.concurrent.CompletableFuture

class LectureSchema(
    private val lectureConverter: LectureConverter,
    private val lectureService: LectureService,
    private val userConverter: UserConverter,
) {
    inner class Query {
        @GraphQLDescription("Get all lectures paged")
        fun lectures(page: Int? = null, size: Int? = null): List<LectureResponse> {
            val pagination = (page to size).pagination()

            return transaction {
                LectureDAO.all()
                    .orderBy(Lectures.createDate to SortOrder.ASC)
                    .limit(pagination.limit, pagination.offset)
                    .map { it.toLecture() }
            }.map { lectureConverter.convert(it) }
        }

        @GraphQLDescription("Get single lecture by its id")
        fun lecture(id: String): LectureResponse? {
            return transaction {
                LectureDAO.findById(id)?.toLecture()
            }?.let {
                lectureConverter.convert(it)
            }
        }
    }

    inner class Mutation {
        @GraphQLDescription("Create lecture with request")
        fun createLecture(request: LectureRequest, env: DataFetchingEnvironment): LectureResponse {
            val principal = env.principal()

            val lecture = lectureService.createLecture(request, principal.userId)

            return lectureConverter.convert(lecture)
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

        @GraphQLDescription("Replace lecture theme with new one (PUT equivalent)")
        fun replaceLectureTheme(
            id: String,
            request: LectureThemeRequest,
            env: DataFetchingEnvironment
        ): LectureResponse {
            val principal = env.principal()
            val lecture = transaction { LectureDAO.findById(id)?.toLecture() }
                ?: throw LectureNotFoundException(id)

            principal.assertLectureWriteAccess(lecture)

            val updatedLecture = lectureService.updateTheme(lecture, request)

            return lectureConverter.convert(updatedLecture)
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

        @GraphQLDescription("Follow Lecture by id")
        fun followLecture(id: String, env: DataFetchingEnvironment): Boolean {
            val principal = env.principal()

            val user = transaction { UserDAO.findById(principal.userId)?.toUser() }
                ?: throw UserNotFoundException(principal.userId)
            val lecture = transaction { LectureDAO.findById(id)?.toLecture() }
                ?: throw LectureNotFoundException(id)

            lectureService.followLecture(user, lecture)

            return true
        }

        @GraphQLDescription("Unfollow lecture by id")
        fun unfollowLecture(id: String, env: DataFetchingEnvironment): Boolean {
            val principal = env.principal()

            val user = transaction { UserDAO.findById(principal.userId)?.toUser() }
                ?: throw UserNotFoundException(principal.userId)
            val lecture = transaction { LectureDAO.findById(id)?.toLecture() }
                ?: throw LectureNotFoundException(id)

            lectureService.unfollowLecture(user, lecture)

            return true
        }
    }

    inner class AuthorDataLoader : KotlinDataLoader<String, UserResponse> {
        override val dataLoaderName: String = "LectureAuthorDataLoader"

        override fun getDataLoader(): DataLoader<String, UserResponse> =
            DataLoaderFactory.newDataLoader { authorIds ->
                CompletableFuture.supplyAsync {
                    val users = transaction {
                        UserDAO.forIds(authorIds).map { it.toUser() }
                    }
                    
                    users.map { userConverter.convert(it) }
                }
            }
    }

    inner class IsFollowedDataLoader : KotlinDataLoader<String, Boolean> {
        override val dataLoaderName: String = "LectureIsFollowedDataLoader"

        override fun getDataLoader(): DataLoader<String, Boolean> =
            DataLoaderFactory.newDataLoader { lectureIds, env ->
                CompletableFuture.supplyAsync {
                    val principal = env.principal()
                    val followedMap = mutableMapOf<String, Boolean>()

                    transaction {
                        lectureIds.forEach { lectureId ->
                            val follow = LectureDAO.find {
                                (LectureFollows.user eq principal.userId) and (LectureFollows.lecture eq lectureId)
                            }.firstOrNull()

                            followedMap[lectureId] = follow != null
                        }
                    }

                    followedMap.values.toList()
                }
            }
    }
}