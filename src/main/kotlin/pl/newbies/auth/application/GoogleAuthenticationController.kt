package pl.newbies.auth.application

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.auth.application.model.GoogleUser
import pl.newbies.auth.domain.UnauthorizedException
import pl.newbies.auth.domain.service.AuthService
import pl.newbies.plugins.inject
import pl.newbies.user.domain.service.UserService
import pl.newbies.user.infrastructure.repository.UserDAO
import pl.newbies.user.infrastructure.repository.Users
import pl.newbies.user.infrastructure.repository.toUser

fun Application.googleAuthentication(oauthClient: HttpClient) {
    val userService by inject<UserService>()
    val authService by inject<AuthService>()

    val config = environment.config
    val userApiUrl = config.property("oauth.google.userUrl").getString()

    authentication {
        oauth("google") {
            urlProvider = { "http://localhost:8081/auth/callback/google" }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "google",
                    authorizeUrl = "https://accounts.google.com/o/oauth2/v2/auth",
                    accessTokenUrl = "https://oauth2.googleapis.com/token",
                    requestMethod = HttpMethod.Post,
                    clientId = config.property("oauth.google.clientId").getString(),
                    clientSecret = config.property("oauth.google.secret").getString(),
                    defaultScopes = listOf("openid", "profile"),
                    passParamsInURL = true,
                )
            }
            client = oauthClient
        }
    }

    routing {
        authenticate("google") {
            get("/oauth/login/google") { /* handled automatically */ }

            get("/oauth/callback/google") {
                val token = call.principal<OAuthAccessTokenResponse.OAuth2>()?.accessToken
                    ?: throw UnauthorizedException("Failed to get google auth token")

                val googleUser = oauthClient.get(userApiUrl) {
                    bearerAuth(token)
                }.body<GoogleUser>()

                val user = transaction {
                    UserDAO.find { Users.googleId eq googleUser.id }.singleOrNull()?.toUser()
                } ?: userService.createUser(
                    nickname = googleUser.name,
                    googleId = googleUser.id,
                )

                val response = authService.generateResponse(user, previousToken = null)

                call.respond(response)
            }
        }
    }
}