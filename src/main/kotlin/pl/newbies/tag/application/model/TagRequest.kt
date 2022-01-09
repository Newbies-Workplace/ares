package pl.newbies.tag.application.model

import kotlinx.serialization.Serializable

@Serializable
data class TagRequest(
    val id: String,
)