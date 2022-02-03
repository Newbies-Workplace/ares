package pl.newbies.util

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.ContentNegotiation
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

fun Application.pongModule() {
    routing {
        get("/ping") {
            call.respond("pong")
        }
    }
}

fun Application.githubModule() {
    install(ContentNegotiation) {
        json()
    }

    routing {
        get("login/oauth/authorize") {
            call.respondRedirect("/login?clientId=test")
        }

        get("login") {
            call.respond("ok")
        }

        post("login/oauth/access_token") {
            call.request.queryParameters["code"]
                ?.takeIf { it == "valid" }
                ?: run {
                    call.respond(HttpStatusCode.Unauthorized, "code invalid")
                    return@post
                }
            val state = call.request.queryParameters["state"]
                ?.takeIf { TestData.githubUsers.map { it.id }.contains(it) }
                ?: run {
                    call.respond(HttpStatusCode.Unauthorized, "state invalid")
                    return@post
                }

            call.respond(
                status = HttpStatusCode.OK,
                message = OauthResponse(accessToken = "validToken-$state")
            )
        }

        get("user") {
            val authorization = call.request.header(HttpHeaders.Authorization)
                ?.takeIf { it.startsWith("Bearer validToken-") }
                ?: run {
                    call.respond(HttpStatusCode.Unauthorized, "Authorization invalid")
                    return@get
                }

            call.respond(TestData.githubUsers.find { it.id == authorization.substringAfter("-") }!!)
        }
    }
}

@Serializable
data class OauthResponse(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("token_type")
    val tokenType: String = "Bearer",
    @SerialName("expires_in")
    val expiresIn: Long = 60L,
    @SerialName("refresh_token")
    val refreshToken: String = ""
)