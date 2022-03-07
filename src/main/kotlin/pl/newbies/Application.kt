package pl.newbies

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import kotlinx.serialization.json.Json
import pl.newbies.auth.authModule
import pl.newbies.common.commonModule
import pl.newbies.lecture.application.lectureRoutes
import pl.newbies.lecture.lectureModule
import pl.newbies.plugins.*
import pl.newbies.tag.application.tagRoutes
import pl.newbies.tag.tagModule
import pl.newbies.user.application.userRoutes
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
            commonModule,
            authModule,
            userModule,
            tagModule,
            lectureModule,
        )
    }

    graphQLModule()

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