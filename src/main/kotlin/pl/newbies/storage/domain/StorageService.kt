package pl.newbies.storage.domain

import io.ktor.http.content.PartData
import io.ktor.http.content.streamProvider
import pl.newbies.common.FileTypeNotSupportedException
import pl.newbies.common.extension
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.createFile

class StorageService(mainStoragePath: String) {

    private val storageRoot = Files.createDirectories(Path(mainStoragePath))
    private val tempDir = Files.createDirectories(storageRoot.resolve(TEMP_DIRECTORY))

    fun cleanupTempDirectory() = runCatching {
        Files.walk(tempDir)
            .map { it.toFile() }
            .forEach { it.delete() }
    }

    fun saveTempFile(fileItem: PartData.FileItem): Path {
        val extension = fileItem.extension
        val name = UUID.randomUUID().toString()

        val newFile = tempDir.resolve("$name.$extension").apply {
            createFile()
        }

        fileItem.streamProvider().use { inp ->
            newFile.toFile().outputStream().buffered().use {
                inp.copyTo(it)
            }
        }

        return newFile
    }

    fun assertSupportedImageType(extension: String) {
        if (extension !in ALLOWED_IMAGE_TYPES) {
            throw FileTypeNotSupportedException(extension, ALLOWED_IMAGE_TYPES)
        }
    }

    //todo tests
    //todo disable temp download
    //todo disable root download
    fun getFile(stringPath: String): File? {
        val path = Path(stringPath).normalize()
        val file = File(storageRoot.toFile(), path.toString())

        if (!file.canonicalPath.startsWith(storageRoot.toFile().canonicalPath)) {
            return null
        }
        if (!file.exists() || file.isDirectory) {
            return null
        }

        return file
    }

    companion object {
        private const val TEMP_DIRECTORY = "temp"

        private val ALLOWED_IMAGE_TYPES = listOf(
            "jpg",
            "png",
            "jpeg",
        )
    }
}