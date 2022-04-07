package pl.newbies.event.application

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.header
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
import pl.newbies.event.application.model.EventRequest
import pl.newbies.event.domain.EventNotFoundException
import pl.newbies.event.domain.model.Event
import pl.newbies.event.domain.service.EventService
import pl.newbies.event.infrastructure.repository.EventDAO
import pl.newbies.event.infrastructure.repository.Events
import pl.newbies.event.infrastructure.repository.toEvent
import pl.newbies.plugins.AresPrincipal
import pl.newbies.plugins.inject
import pl.newbies.storage.application.FileUrlConverter
import pl.newbies.storage.domain.StorageService
import pl.newbies.storage.domain.model.EventDirectoryResource
import pl.newbies.storage.domain.model.EventImageFileResource

fun Application.eventRoutes() {
    val eventService: EventService by inject()
    val storageService: StorageService by inject()
    val eventConverter: EventConverter by inject()
    val fileUrlConverter: FileUrlConverter by inject()

    routing {
        route("/api/v1/events") {
            get {
                val pagination = call.pagination()

                val events = transaction {
                    EventDAO.all()
                        .orderBy(Events.createDate to SortOrder.ASC)
                        .limit(pagination.limit, pagination.offset)
                        .map { it.toEvent() }
                }.map { eventConverter.convert(it) }

                call.respond(events)
            }

            get("/{id}") {
                val id = call.parameters.getOrFail("id")

                val event = transaction { EventDAO.findById(id)?.toEvent() }
                    ?: throw EventNotFoundException(id)

                call.respond(eventConverter.convert(event))
            }

            authenticate("jwt") {
                post {
                    val request = call.receive<EventRequest>()
                    val principal = call.principal<AresPrincipal>()!!

                    val createdEvent = eventService.createEvent(
                        request = request,
                        authorId = principal.userId,
                    )

                    call.respond(eventConverter.convert(createdEvent))
                }

                put("/{id}") {
                    val id = call.parameters.getOrFail("id")
                    val principal = call.principal<AresPrincipal>()!!
                    val request = call.receive<EventRequest>()
                    val event = transaction { EventDAO.findById(id)?.toEvent() }
                        ?: throw EventNotFoundException(id)

                    principal.assertEventWriteAccess(event)

                    val updatedEvent = eventService.updateEvent(
                        event = event,
                        request = request,
                    )

                    call.respond(eventConverter.convert(updatedEvent))
                }

                put("/{id}/theme/image") {
                    val id = call.parameters.getOrFail("id")
                    val principal = call.principal<AresPrincipal>()!!
                    val event = transaction { EventDAO.findById(id)?.toEvent() }
                        ?: throw EventNotFoundException(id)

                    principal.assertEventWriteAccess(event)

                    val part = (call.receiveMultipart().readPart() as? PartData.FileItem)
                        ?: throw BadRequestException("Part is not a file.")
                    val contentLength = call.request.header(HttpHeaders.ContentLength)?.toLongOrNull()
                        ?: throw BadRequestException("Content-Length header not present.")

                    storageService.assertFileSize(contentLength)
                    storageService.assertSupportedImageType(part.extension)

                    val fileResource = eventService.getThemeImageFileResource(event)
                        ?.also { res -> storageService.removeResource(res) }
                        ?: EventImageFileResource(event.id, "image.webp")

                    val tempFileResource = storageService.saveTempFile(part)

                    call.respond(fileUrlConverter.convert(call, fileResource))

                    storageService.saveImage(tempFileResource, fileResource)
                    storageService.removeResource(tempFileResource)
                    eventService.updateThemeImage(event, fileResource)
                }

                delete("/{id}/theme/image") {
                    val id = call.parameters.getOrFail("id")
                    val principal = call.principal<AresPrincipal>()!!
                    val event = transaction { EventDAO.findById(id)?.toEvent() }
                        ?: throw EventNotFoundException(id)

                    principal.assertEventWriteAccess(event)

                    eventService.getThemeImageFileResource(event)?.let { fileResource ->
                        storageService.removeResource(fileResource)
                        eventService.updateThemeImage(event, null)
                    }

                    call.respond(HttpStatusCode.OK)
                }

                delete("/{id}") {
                    val id = call.parameters.getOrFail("id")
                    val principal = call.principal<AresPrincipal>()!!
                    val event = transaction { EventDAO.findById(id)?.toEvent() }
                        ?: throw EventNotFoundException(id)

                    principal.assertEventWriteAccess(event)

                    eventService.deleteEvent(event)

                    storageService.removeDirectory(EventDirectoryResource(event.id))

                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}

fun AresPrincipal.assertEventWriteAccess(event: Event) {
    if (event.authorId != userId) {
        throw ForbiddenException(
            userId = userId,
            entityId = event.id,
        )
    }
}