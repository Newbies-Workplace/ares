package pl.newbies.storage.domain.model

open class DirectoryResource(
    val path: String
)

object TempDirectoryResource : DirectoryResource("temp/")

data class EventDirectoryResource(
    val eventId: String
) : DirectoryResource("events/$eventId")