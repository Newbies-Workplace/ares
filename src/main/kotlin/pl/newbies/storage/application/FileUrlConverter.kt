package pl.newbies.storage.application

import pl.newbies.storage.application.model.FileUrlResponse
import pl.newbies.storage.domain.model.FileResource

class FileUrlConverter(
    private val fileServiceUrl: String
) {

    fun convert(file: FileResource): FileUrlResponse =
        convert(file.pathWithName)

    fun convert(pathWithName: String): FileUrlResponse {
        val fullFileUrl = "$fileServiceUrl${StorageController.DOWNLOAD_FILE_V1_PATH}$pathWithName"

        return FileUrlResponse(fullFileUrl)
    }
}