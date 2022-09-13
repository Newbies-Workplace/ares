package pl.newbies.lecture.application

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import graphql.schema.DataFetchingEnvironment
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.common.principal
import pl.newbies.event.application.assertEventWriteAccess
import pl.newbies.event.domain.EventNotFoundException
import pl.newbies.event.infrastructure.repository.EventDAO
import pl.newbies.event.infrastructure.repository.toEvent
import pl.newbies.lecture.application.model.LectureFilter
import pl.newbies.lecture.application.model.LectureRequest
import pl.newbies.lecture.application.model.LectureResponse
import pl.newbies.lecture.domain.LectureNotFoundException
import pl.newbies.lecture.domain.service.LectureService
import pl.newbies.lecture.infrastructure.repository.LectureDAO
import pl.newbies.lecture.infrastructure.repository.toLecture
import com.expediagroup.graphql.server.operations.Mutation as GraphQLMutation
import com.expediagroup.graphql.server.operations.Query as GraphQLQuery

class LectureSchema(
    private val lectureConverter: LectureConverter,
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
    }
}