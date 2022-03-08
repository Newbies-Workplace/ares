package pl.newbies.storage.application

import io.ktor.server.application.Application
import io.ktor.server.http.content.files
import io.ktor.server.http.content.static
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.storageRoutes() {

    routing {
        static {
            files(".")
        }

        route("/api/v1/file/{name}") {
            //todo
        }
    }
}