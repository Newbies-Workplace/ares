package pl.newbies.auth.application

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.auth.domain.UnauthorizedException
import pl.newbies.auth.domain.service.AuthService
import pl.newbies.auth.infrastructure.repository.RefreshTokenDAO
import pl.newbies.auth.infrastructure.repository.toRefreshToken
import pl.newbies.common.ForbiddenException
import pl.newbies.plugins.AresPrincipal
import pl.newbies.plugins.inject
import pl.newbies.user.domain.UserNotFoundException
import pl.newbies.user.infrastructure.repository.UserDAO
import pl.newbies.user.infrastructure.repository.toUser

fun Application.authenticationRoutes() {
    val authService: AuthService by inject()

    routing {
        post("/api/v1/refresh") {
            val refreshToken = call.receive<String>()
            val token = transaction {
                RefreshTokenDAO.findById(refreshToken)?.toRefreshToken()
            } ?: throw UnauthorizedException("Refresh token not found")

            if (token.isUsed) {
                authService.deleteTokenFamily(token)

                throw UnauthorizedException("Toked has been used already")
            }

            if (token.dateExpired < Clock.System.now()) {
                throw UnauthorizedException("Token has expired")
            }

            val user = transaction {
                UserDAO.findById(token.userId)?.toUser()
            } ?: throw UserNotFoundException(token.userId)

            val response = authService.generateResponse(user, token)

            call.respond(response)
        }

        authenticate("jwt") {
            delete("/api/v1/logout") {
                val refreshToken = call.receive<String>()
                val principal = call.principal<AresPrincipal>()!!
                val token = transaction {
                    RefreshTokenDAO.findById(refreshToken)?.toRefreshToken()
                } ?: throw UnauthorizedException("Refresh token not found")

                if (token.isUsed) {
                    authService.deleteTokenFamily(token)

                    throw UnauthorizedException("Toked has been used already")
                }

                if (token.userId != principal.userId) {
                    throw ForbiddenException(userId = token.userId, entityId = refreshToken)
                }

                authService.deleteTokenFamily(token)

                call.respond(HttpStatusCode.OK)
            }
        }
    }
}