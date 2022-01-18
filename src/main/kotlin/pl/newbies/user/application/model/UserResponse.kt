package pl.newbies.user.application.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    val id: String,
    val nickname: String,
    val description: String?,
    val contact: ContactResponse,
    val createDate: Instant,
    val updateDate: Instant,
)

@Serializable
data class ContactResponse(
    val github: String?,
    val linkedin: String?,
    val mail: String?,
    val twitter: String?,
)