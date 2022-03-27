package pl.newbies.lecture.application

import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.util.getOrFail
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.common.ForbiddenException
import pl.newbies.common.extension
import pl.newbies.common.pagination
import pl.newbies.lecture.application.model.LectureRequest
import pl.newbies.lecture.domain.LectureNotFoundException
import pl.newbies.lecture.domain.model.Lecture
import pl.newbies.lecture.domain.service.LectureService
import pl.newbies.lecture.infrastructure.repository.LectureDAO
import pl.newbies.lecture.infrastructure.repository.Lectures
import pl.newbies.lecture.infrastructure.repository.toLecture
import pl.newbies.plugins.AresPrincipal
import pl.newbies.plugins.inject
import pl.newbies.storage.application.FileUrlConverter
import pl.newbies.storage.domain.StorageService
import pl.newbies.storage.domain.model.LectureDirectoryResource
import pl.newbies.storage.domain.model.LectureImageFileResource

fun Application.lectureRoutes() {
    val lectureService: LectureService by inject()
    val storageService: StorageService by inject()
    val lectureConverter: LectureConverter by inject()
    val fileUrlConverter: FileUrlConverter by inject()

    routing {
        route("/api/v1/lectures") {
            get {
                val pagination = call.pagination()

                val lectures = transaction {
                    LectureDAO.all()
                        .orderBy(Lectures.createDate to SortOrder.ASC)
                        .limit(pagination.limit, pagination.offset)
                        .map { it.toLecture() }
                }.map { lectureConverter.convert(it) }

                call.respond(lectures)
            }

            get("/{id}") {
                val id = call.parameters.getOrFail("id")

                val lecture = transaction { LectureDAO.findById(id)?.toLecture() }
                    ?: throw LectureNotFoundException(id)

                call.respond(lectureConverter.convert(lecture))
            }

            authenticate("jwt") {
                post {
                    val request = call.receive<LectureRequest>()
                    val principal = call.principal<AresPrincipal>()!!

                    val createdLecture = lectureService.createLecture(
                        request = request,
                        authorId = principal.userId,
                    )

                    call.respond(lectureConverter.convert(createdLecture))
                }

                put("/{id}") {
                    val id = call.parameters.getOrFail("id")
                    val principal = call.principal<AresPrincipal>()!!
                    val request = call.receive<LectureRequest>()
                    val lecture = transaction { LectureDAO.findById(id)?.toLecture() }
                        ?: throw LectureNotFoundException(id)

                    principal.assertLectureWriteAccess(lecture)

                    val updatedLecture = lectureService.updateLecture(
                        lecture = lecture,
                        request = request,
                    )

                    call.respond(lectureConverter.convert(updatedLecture))
                }

                put("/{id}/theme/image") {
                    val id = call.parameters.getOrFail("id")
                    val principal = call.principal<AresPrincipal>()!!
                    val lecture = transaction { LectureDAO.findById(id)?.toLecture() }
                        ?: throw LectureNotFoundException(id)

                    principal.assertLectureWriteAccess(lecture)

                    val part = (call.receiveMultipart().readPart() as? PartData.FileItem)
                        ?: throw BadRequestException("Part is not a file.")

                    storageService.assertSupportedImageType(part.extension)

                    val fileResource = lectureService.getThemeImageFileResource(lecture)
                        ?.also { res -> storageService.removeResource(res) }
                        ?: LectureImageFileResource(lecture.id, "image.webp")

                    val tempFileResource = storageService.saveTempFile(part)

                    call.respond(fileUrlConverter.convert(call, fileResource))

                    storageService.saveImage(tempFileResource, fileResource)
                    storageService.removeResource(tempFileResource)
                    lectureService.updateThemeImage(lecture, fileResource)
                }

                delete("/{id}/theme/image") {
                    val id = call.parameters.getOrFail("id")
                    val principal = call.principal<AresPrincipal>()!!
                    val lecture = transaction { LectureDAO.findById(id)?.toLecture() }
                        ?: throw LectureNotFoundException(id)

                    principal.assertLectureWriteAccess(lecture)

                    lectureService.getThemeImageFileResource(lecture)?.let { fileResource ->
                        storageService.removeResource(fileResource)
                        lectureService.updateThemeImage(lecture, null)
                    }

                    call.respond(HttpStatusCode.OK)
                }

                delete("/{id}") {
                    val id = call.parameters.getOrFail("id")
                    val principal = call.principal<AresPrincipal>()!!
                    val lecture = transaction { LectureDAO.findById(id)?.toLecture() }
                        ?: throw LectureNotFoundException(id)

                    principal.assertLectureWriteAccess(lecture)

                    lectureService.deleteLecture(lecture)

                    storageService.removeDirectory(LectureDirectoryResource(lecture.id))

                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}

fun AresPrincipal.assertLectureWriteAccess(lecture: Lecture) {
    if (lecture.authorId != userId) {
        throw ForbiddenException(
            userId = userId,
            entityId = lecture.id,
        )
    }
}