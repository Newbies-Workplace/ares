package pl.newbies.storage.application

import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin
import pl.newbies.storage.application.model.FileUrlResponse
import pl.newbies.storage.domain.model.FileResource

class FileUrlConverter {

    fun convert(call: ApplicationCall, file: FileResource): FileUrlResponse {
        val connectionPoint = call.request.origin

        val scheme = connectionPoint.scheme
        val host = connectionPoint.host
        val port = connectionPoint.port

        val fullFileUrl = "$scheme://$host:$port${StorageController.DOWNLOAD_FILE_V1_PATH}${file.pathWithName}"

        return FileUrlResponse(fullFileUrl)
    }
}