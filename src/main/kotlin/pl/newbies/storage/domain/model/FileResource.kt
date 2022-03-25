package pl.newbies.storage.domain.model

data class FileResource(
    val storagePath: String,
    val name: String,
) {

    val pathWithName: String
        get() = "$storagePath$name"
}