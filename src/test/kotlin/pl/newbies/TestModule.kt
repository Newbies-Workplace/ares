package pl.newbies

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.auth.domain.service.AuthService
import pl.newbies.plugins.inject
import pl.newbies.user.domain.service.UserService
import pl.newbies.user.infrastructure.repository.UserDAO
import pl.newbies.user.infrastructure.repository.Users
import pl.newbies.user.infrastructure.repository.toUser

fun Application.testModule() {
    val userService by inject<UserService>()
    val authService by inject<AuthService>()

    routing {
        get("test/login") {
            val req = call.receive<Map<String, String>>()

            val user = transaction {
                UserDAO.find { Users.githubId eq req["githubId"]!! }.singleOrNull()?.toUser()
            } ?: userService.createUser(
                nickname = req["nickame"]!!,
                githubId = req["githubId"]!!,
            )

            val response = authService.generateResponse(user, refreshToken = null)

            call.respond(response)
        }
    }
}