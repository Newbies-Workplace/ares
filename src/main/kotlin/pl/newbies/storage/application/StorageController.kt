package pl.newbies.storage.application

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopPreparing
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import pl.newbies.plugins.inject
import pl.newbies.storage.application.StorageController.DOWNLOAD_FILE_V1_PATH
import pl.newbies.storage.domain.StorageService

fun Application.storageRoutes() {
    val service: StorageService by inject()

    environment.monitor.subscribe(ApplicationStopPreparing) {
        service.cleanupTempDirectory()
    }

    routing {
        get("$DOWNLOAD_FILE_V1_PATH/{path...}") {
            val path = call.parameters.getAll("path")?.joinToString("/")!!

            service.getFile(path)?.let {
                call.respondFile(it)
            } ?: call.respond(HttpStatusCode.NotFound)
        }
    }
}

object StorageController {
    const val DOWNLOAD_FILE_V1_PATH = "/api/v1/files/"
}