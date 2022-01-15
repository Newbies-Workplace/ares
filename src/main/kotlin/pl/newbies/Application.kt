package pl.newbies

import io.ktor.server.application.*
import io.ktor.server.application.Application
import pl.newbies.auth.authModule
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
fun Application.module() {
    configureSecurity()
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

    userRoutes()
    tagRoutes()
    lectureRoutes()
}
