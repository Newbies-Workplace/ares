package pl.newbies.auth.application

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.*
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.auth.application.model.GithubUser
import pl.newbies.auth.domain.UnauthorizedException
import pl.newbies.auth.domain.service.AuthService
import pl.newbies.plugins.inject
import pl.newbies.user.domain.service.UserService
import pl.newbies.user.infrastructure.repository.UserDAO
import pl.newbies.user.infrastructure.repository.Users
import pl.newbies.user.infrastructure.repository.toUser

/**
 * GitHub DEV implementation, used only for testing
 */
fun Application.devGithubAuthentication(oauthClient: HttpClient) {
    val userService by inject<UserService>()
    val authService by inject<AuthService>()

    val config = environment.config
    val userApiUrl = config.property("oauth.devgithub.userUrl").getString()

    authentication {
        oauth("devgithub") {
            urlProvider = { "http://localhost:8080/auth/callback/devgithub" }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "devgithub",
                    authorizeUrl = "https://github.com/login/oauth/authorize",
                    accessTokenUrl = "https://github.com/login/oauth/access_token",
                    requestMethod = HttpMethod.Post,
                    clientId = config.property("oauth.devgithub.clientId").getString(),
                    clientSecret = config.property("oauth.devgithub.secret").getString(),
                    defaultScopes = listOf("user:email", "read:user"),
                    passParamsInURL = true,
                )
            }
            client = oauthClient
        }
    }

    routing {
        authenticate("devgithub") {
            get("/api/oauth/login/devgithub") { /* handled automatically */ }

            get("/api/oauth/callback/devgithub") {
                val token = call.principal<OAuthAccessTokenResponse.OAuth2>()?.accessToken
                    ?: throw UnauthorizedException("Failed to get github auth token")

                val githubUser = oauthClient.get(userApiUrl) {
                    bearerAuth(token)
                }.body<GithubUser>()

                val user = transaction {
                    UserDAO.find { Users.devGithubId eq githubUser.id }.singleOrNull()?.toUser()
                } ?: userService.createUser(
                    nickname = githubUser.login,
                    devgithubId = githubUser.id,
                )

                val response = authService.generateResponse(user, previousToken = null)

                call.respond(response)
            }
        }
    }
}