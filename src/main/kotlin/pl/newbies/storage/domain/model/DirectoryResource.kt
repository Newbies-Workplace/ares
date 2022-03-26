package pl.newbies.storage.domain.model

open class DirectoryResource(
    val path: String
)

object TempDirectoryResource : DirectoryResource("temp/")

data class LectureDirectoryResource(
    val lectureId: String
) : DirectoryResource("lectures/$lectureId/")