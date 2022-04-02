package pl.newbies.plugins

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.CORS

fun Application.configureHTTP() {
    install(CORS) {
        anyHost()
        allowNonSimpleContentTypes = true

        method(HttpMethod.Options)
        method(HttpMethod.Put)
        method(HttpMethod.Patch)
        method(HttpMethod.Delete)

        header(HttpHeaders.Authorization)
        header(HttpHeaders.ContentLength)
    }
}
