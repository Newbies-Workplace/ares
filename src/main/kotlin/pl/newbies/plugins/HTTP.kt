package pl.newbies.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.CORS

fun Application.configureHTTP() {
    install(CORS) {
        anyHost()
    }
}
