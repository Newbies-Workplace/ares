package pl.newbies.auth.application.model

import kotlinx.serialization.Serializable

@Serializable
class PropertiesResponse(
    val isInitialized: Boolean,
)