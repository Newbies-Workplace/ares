package pl.newbies.user.application

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.util.getOrFail
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.plugins.AresPrincipal
import pl.newbies.plugins.inject
import pl.newbies.user.application.model.UserRequest
import pl.newbies.user.domain.UserNotFoundException
import pl.newbies.user.domain.service.UserService
import pl.newbies.user.infrastructure.repository.UserDAO
import pl.newbies.user.infrastructure.repository.toUser

fun Application.userRoutes() {
    val userConverter by inject<UserConverter>()
    val userService by inject<UserService>()

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

                put("/@me") {
                    val id = call.principal<AresPrincipal>()!!.userId
                    val userRequest = call.receive<UserRequest>()

                    val updatedUser = userService.updateUser(id, userRequest)

                    call.respond(userConverter.convert(updatedUser))
                }
            }
        }
    }
}