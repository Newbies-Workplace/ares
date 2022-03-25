package pl.newbies.storage.application.model

import kotlinx.serialization.Serializable

@Serializable
data class FileUrlResponse(
    val url: String,
)