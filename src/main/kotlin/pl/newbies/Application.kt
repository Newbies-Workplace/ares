package pl.newbies

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import pl.newbies.auth.authModule
import pl.newbies.lecture.application.lectureRoutes
import pl.newbies.lecture.application.lectureSchema
import pl.newbies.lecture.lectureModule
import pl.newbies.plugins.*
import pl.newbies.tag.application.tagRoutes
import pl.newbies.tag.application.tagSchema
import pl.newbies.tag.tagModule
import pl.newbies.user.application.userRoutes
import pl.newbies.user.application.userSchema
import pl.newbies.user.userModule

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused")
fun Application.module(oauthClient: HttpClient = oauthHttpClient) {
    configureSecurity(oauthClient)
    configureHTTP()
    configureSerialization()
    configureDatabase()
    configureStatusPages()

    install(KoinPlugin) {
        modules(
            configModule(environment.config),
            authModule,
            userModule,
            tagModule,
            lectureModule,
        )
    }

    install(GraphQLPlugin) {
        playground = true
        useDefaultPrettyPrinter = true

        wrap {
            authenticate("jwt", optional = true, build = it)
        }

        context { call ->
            call.authentication.principal<AresPrincipal>()?.let {
                inject(it)
            }
        }

        schema {
            stringScalar<Instant> {
                serialize = { date -> date.toString() }
                deserialize = { dateString -> Instant.parse(dateString) }
            }

            userSchema()
            tagSchema()
            lectureSchema()
        }
    }

    userRoutes()
    tagRoutes()
    lectureRoutes()
}

val oauthHttpClient = HttpClient(Apache) {
    install(ContentNegotiation) {
        json(Json {
            encodeDefaults = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
}