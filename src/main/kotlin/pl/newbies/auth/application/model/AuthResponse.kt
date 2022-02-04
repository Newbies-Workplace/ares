package pl.newbies.auth.application.model

import kotlinx.serialization.Serializable
import pl.newbies.user.application.model.UserResponse

@Serializable
data class AuthResponse(
    val username: String,
    val roles: List<String> = emptyList(),
    val accessToken: String,
    val refreshToken: String = "todo",
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val user: UserResponse,
    val properties: PropertiesResponse,
)