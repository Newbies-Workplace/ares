package pl.newbies.lecture.application

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.util.getOrFail
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.common.ForbiddenException
import pl.newbies.common.query
import pl.newbies.event.application.assertEventWriteAccess
import pl.newbies.event.domain.EventNotFoundException
import pl.newbies.event.infrastructure.repository.EventDAO
import pl.newbies.event.infrastructure.repository.toEvent
import pl.newbies.lecture.application.model.LectureFilter
import pl.newbies.lecture.application.model.LectureRequest
import pl.newbies.lecture.domain.LectureNotFoundException
import pl.newbies.lecture.domain.model.Lecture
import pl.newbies.lecture.domain.service.LectureService
import pl.newbies.lecture.infrastructure.repository.LectureDAO
import pl.newbies.lecture.infrastructure.repository.toLecture
import pl.newbies.plugins.AresPrincipal
import pl.newbies.plugins.inject

fun Application.lectureRoutes() {
    val lectureService: LectureService by inject()
    val lectureConverter: LectureConverter by inject()

    routing {
        route("/api/v1") {
            authenticate("jwt") {
                post("/events/{id}/lectures") {
                    val id = call.parameters.getOrFail("id")
                    val request = call.receive<LectureRequest>()
                    val principal = call.principal<AresPrincipal>()!!
                    val event = transaction { EventDAO.findById(id)?.toEvent() }
                        ?: throw EventNotFoundException(id)

                    principal.assertEventWriteAccess(event)

                    val createdLecture = lectureService.createLecture(request, event, principal.userId)

                    call.respond(HttpStatusCode.Created, lectureConverter.convert(createdLecture))
                }

                put("/lectures/{id}") {
                    val id = call.parameters.getOrFail("id")
                    val request = call.receive<LectureRequest>()
                    val principal = call.principal<AresPrincipal>()!!
                    val lecture = transaction { LectureDAO.findById(id)?.toLecture() }
                        ?: throw LectureNotFoundException(id)

                    principal.assertLectureWriteAccess(lecture)

                    val updatedLecture = lectureService.updateLecture(lecture, request)

                    call.respond(lectureConverter.convert(updatedLecture))
                }

                delete("/lectures/{id}") {
                    val id = call.parameters.getOrFail("id")
                    val principal = call.principal<AresPrincipal>()!!
                    val lecture = transaction { LectureDAO.findById(id)?.toLecture() }
                        ?: throw LectureNotFoundException(id)

                    principal.assertLectureWriteAccess(lecture)

                    lectureService.deleteLecture(lecture)

                    call.respond(HttpStatusCode.OK)
                }
            }

            get("/lectures") {
                val filter: LectureFilter = call.query("filter")
                    ?: throw BadRequestException("No 'filter' provided")

                val lectures = lectureService.getLectures(filter)

                call.respond(lectures.map { lectureConverter.convert(it) })
            }

            get("/lectures/{id}") {
                val id = call.parameters.getOrFail("id")

                val lecture = transaction {
                    LectureDAO.findById(id)?.toLecture()
                } ?: throw LectureNotFoundException(id = id)

                call.respond(lectureConverter.convert(lecture))
            }
        }
    }
}

fun AresPrincipal.assertLectureWriteAccess(lecture: Lecture) {
    if (lecture.author.id != userId && !lecture.speakers.map { it.id }.contains(userId)) {
        throw ForbiddenException(
            userId = userId,
            entityId = lecture.id,
        )
    }
}