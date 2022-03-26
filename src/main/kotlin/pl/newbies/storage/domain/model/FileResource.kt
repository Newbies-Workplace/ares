package pl.newbies.storage.domain.model

open class FileResource(
    val storagePath: String,
    val nameWithExtension: String,
) {

    val pathWithName: String
        get() = "$storagePath$nameWithExtension"
}

class TempFileResource(
    nameWithExtension: String
): FileResource("temp/", nameWithExtension)

class LectureImageFileResource(
    lectureId: String,
    nameWithExtension: String,
): FileResource("lectures/$lectureId/", nameWithExtension)