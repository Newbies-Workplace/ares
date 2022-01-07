package pl.newbies.user.application.model

import java.time.Instant

data class UserResponse(
    val id: String,
    val nickname: String,
    val description: String?,
    val contact: ContactResponse,
    val createDate: Instant,
)

data class ContactResponse(
    val github: String?,
    val linkedin: String?,
    val mail: String?,
    val twitter: String?,
)