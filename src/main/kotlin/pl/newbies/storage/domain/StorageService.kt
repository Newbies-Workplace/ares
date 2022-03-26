package pl.newbies.storage.domain

import io.ktor.http.content.PartData
import io.ktor.http.content.streamProvider
import pl.newbies.common.FileTypeNotSupportedException
import pl.newbies.common.extension
import pl.newbies.storage.domain.model.DirectoryResource
import pl.newbies.storage.domain.model.FileResource
import pl.newbies.storage.domain.model.TempDirectoryResource
import pl.newbies.storage.domain.model.TempFileResource
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import javax.imageio.ImageIO
import kotlin.io.path.*

class StorageService(mainStoragePath: String) {

    private val storageRoot = Files.createDirectories(Path(mainStoragePath))
    private val tempDir = Files.createDirectories(storageRoot.resolve(TEMP_DIRECTORY))

    fun cleanupTempDirectory() = runCatching {
        removeDirectory(TempDirectoryResource)
    }

    fun saveTempFile(fileItem: PartData.FileItem): TempFileResource {
        val extension = fileItem.extension
        val name = UUID.randomUUID().toString()
        val fullName = "$name.$extension"

        val newFile = tempDir.resolve(fullName).createFile()

        fileItem.streamProvider().use { inp ->
            newFile.toFile().outputStream().buffered().use {
                inp.copyTo(it)
            }
        }

        return TempFileResource(fullName)
    }

    fun saveFile(tempFileResource: TempFileResource, targetResource: FileResource) {
        storageRoot.resolve(targetResource.storagePath).createDirectories()

        val originPath = storageRoot.resolve(tempFileResource.pathWithName)
        val targetPath = storageRoot.resolve(targetResource.pathWithName)

        // ImageIO strips image metadata on read call
        val image = ImageIO.read(originPath.toFile())
        ImageIO.write(image, targetPath.extension, targetPath.toFile())
    }

    fun removeResource(resource: FileResource) {
        assertPathInStorage(storageRoot.resolve(resource.pathWithName))

        storageRoot.resolve(resource.pathWithName).deleteIfExists()
    }

    fun removeDirectory(resource: DirectoryResource) {
        val path: Path = storageRoot.resolve(resource.path).normalize()

        assertPathInStorage(path)

        Files.walk(path)
            .map { it.toFile() }
            .forEach { it.delete() }
    }

    fun assertSupportedImageType(extension: String) {
        if (extension.lowercase() !in ALLOWED_IMAGE_TYPES) {
            throw FileTypeNotSupportedException(extension, ALLOWED_IMAGE_TYPES)
        }
    }

    fun getFile(stringPath: String): File? {
        val path = Path(stringPath).normalize()
        val file = File(storageRoot.toFile(), path.toString())

        // Make sure file is somewhere in the storage root
        if (!file.canonicalPath.startsWith(storageRoot.toFile().canonicalPath)) {
            return null
        }
        // Make sure user can't download temp files
        if (file.canonicalPath.startsWith(tempDir.toFile().canonicalPath)) {
            return null
        }
        if (!file.exists() || file.isDirectory) {
            return null
        }

        return file
    }

    private fun assertPathInStorage(path: Path) {
        if (!path.normalize().toFile().canonicalPath.startsWith(storageRoot.toFile().canonicalPath)) {
            throw SecurityException("Requested path should not be accessible")
        }
    }

    companion object {
        private const val TEMP_DIRECTORY = "temp"

        private val ALLOWED_IMAGE_TYPES = listOf(
            "jpg",
            "png",
            "jpeg",
            "webp",
        )
    }
}