package pl.newbies.plugins

import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.CORS

fun Application.configureHTTP() {
    install(CORS) {
        anyHost()
        host("10.10.10.10:8080")
        allowNonSimpleContentTypes = true
        method(HttpMethod.Options)
        method(HttpMethod.Put)
        method(HttpMethod.Patch)
        method(HttpMethod.Delete)
    }
}
