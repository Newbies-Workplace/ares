package pl.newbies.auth.application.model

import kotlinx.serialization.Serializable

@Serializable
data class GoogleUser(
    val id: String,
    val name: String,
)