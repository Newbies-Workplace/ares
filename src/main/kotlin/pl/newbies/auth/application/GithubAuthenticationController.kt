package pl.newbies.auth.application

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.*
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.auth.application.model.GithubUser
import pl.newbies.auth.domain.UnauthorizedException
import pl.newbies.auth.domain.service.AuthService
import pl.newbies.plugins.inject
import pl.newbies.user.domain.service.UserService
import pl.newbies.user.infrastructure.repository.UserDAO
import pl.newbies.user.infrastructure.repository.Users
import pl.newbies.user.infrastructure.repository.toUser

fun Application.githubAuthentication() {
    val userService by inject<UserService>()
    val authService by inject<AuthService>()

    val config = environment.config
    val userApiUrl = config.property("oauth.github.userUrl").getString()
    val httpClient = HttpClient(Apache) {
        install(ContentNegotiation) {
            json(Json {
                encodeDefaults = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    authentication {
        oauth("github") {
            urlProvider = { "http://jeteo.newbies.pl/auth/callback/github" }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "github",
                    authorizeUrl = "https://github.com/login/oauth/authorize",
                    accessTokenUrl = "https://github.com/login/oauth/access_token",
                    requestMethod = HttpMethod.Post,
                    clientId = config.property("oauth.github.clientId").getString(),
                    clientSecret = config.property("oauth.github.secret").getString(),
                    defaultScopes = listOf("user:email", "read:user"),
                )
            }
            client = httpClient
        }
    }

    routing {
        authenticate("github") {
            get("/oauth/login/github") { /* handled automatically */ }

            get("/oauth/callback/github") {
                val token = call.principal<OAuthAccessTokenResponse.OAuth2>()?.accessToken
                    ?: throw UnauthorizedException("Failed to get github auth token")

                val githubUser: GithubUser = httpClient.get(userApiUrl) {
                    bearerAuth(token)
                }.body()

                val user = transaction {
                    UserDAO.find { Users.githubId eq githubUser.id }.singleOrNull()?.toUser()
                } ?: userService.createUser(
                    nickname = githubUser.login,
                    githubId = githubUser.id,
                )

                val response = authService.generateResponse(user, refreshToken = null)

                call.respond(response)
            }
        }
    }
}