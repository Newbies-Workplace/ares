package pl.newbies.user.application

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
import kotlinx.serialization.json.JsonElement
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.common.extension
import pl.newbies.common.nanoId
import pl.newbies.plugins.AresPrincipal
import pl.newbies.plugins.inject
import pl.newbies.storage.application.FileUrlConverter
import pl.newbies.storage.domain.StorageService
import pl.newbies.storage.domain.model.UserAvatarImageFileResource
import pl.newbies.user.application.model.UserRequest
import pl.newbies.user.domain.UserNotFoundException
import pl.newbies.user.domain.service.UserService
import pl.newbies.user.infrastructure.repository.UserDAO
import pl.newbies.user.infrastructure.repository.toUser

fun Application.userRoutes() {
    val userConverter by inject<UserConverter>()
    val userService by inject<UserService>()
    val storageService by inject<StorageService>()
    val fileUrlConverter: FileUrlConverter by inject()

    routing {
        route("/api/v1/users") {
            get("/{id}") {
                val id = call.parameters.getOrFail("id")
                val user = transaction { UserDAO.findById(id)?.toUser() }
                    ?: throw UserNotFoundException(id)

                call.respond(userConverter.convert(user))
            }

            authenticate("jwt") {
                get("/@me") {
                    val id = call.principal<AresPrincipal>()!!.userId
                    val user = transaction { UserDAO.findById(id)?.toUser() }
                        ?: throw UserNotFoundException(id)

                    call.respond(userConverter.convert(user))
                }

                patch("/@me") {
                    val id = call.principal<AresPrincipal>()!!.userId
                    val changes = call.receive<JsonElement>()
                    val user = transaction { UserDAO.findById(id)?.toUser() }
                        ?: throw UserNotFoundException(id)

                    val updatedUser = userService.updateUser(user, changes)

                    call.respond(userConverter.convert(updatedUser))
                }

                put("/@me") {
                    val id = call.principal<AresPrincipal>()!!.userId
                    val userRequest = call.receive<UserRequest>()

                    val updatedUser = userService.replaceUser(id, userRequest)

                    call.respond(userConverter.convert(updatedUser))
                }

                route("/@me/avatar") {
                    put {
                        val principal = call.principal<AresPrincipal>()!!
                        val user = transaction { UserDAO.findById(principal.userId)?.toUser() }
                            ?: throw UserNotFoundException(principal.userId)

                        val part = (call.receiveMultipart().readPart() as? PartData.FileItem)
                            ?: throw BadRequestException("Part is not a file.")
                        val contentLength = call.request.header(HttpHeaders.ContentLength)?.toLongOrNull()
                            ?: throw BadRequestException("Content-Length header not present.")

                        storageService.assertFileSize(contentLength)
                        storageService.assertSupportedImageType(part.extension)

                        val fileResource = userService.getAvatarFileResource(user)
                            ?.also { res -> storageService.removeResource(res) }
                            ?: UserAvatarImageFileResource(user.id, "${nanoId()}.webp")

                        val tempFileResource = storageService.saveTempFile(part)

                        call.respond(fileUrlConverter.convert(fileResource))
                        storageService.saveImage(tempFileResource, fileResource)
                        storageService.removeResource(tempFileResource)
                        userService.updateUserAvatar(user, fileResource)
                    }

                    delete {
                        val principal = call.principal<AresPrincipal>()!!
                        val user = transaction { UserDAO.findById(principal.userId)?.toUser() }
                            ?: throw UserNotFoundException(principal.userId)

                        userService.getAvatarFileResource(user)?.let { fileResource ->
                            storageService.removeResource(fileResource)
                            userService.updateUserAvatar(user, null)
                        }

                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }
    }
}