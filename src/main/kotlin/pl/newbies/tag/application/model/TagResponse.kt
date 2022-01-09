package pl.newbies.tag.application.model

import kotlinx.serialization.Serializable

@Serializable
data class TagResponse(
    val id: String,
    val name: String,
)