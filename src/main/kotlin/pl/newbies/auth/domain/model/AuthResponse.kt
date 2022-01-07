package pl.newbies.auth.domain.model

data class AuthResponse(
    val username: String,
    val roles: List<String> = emptyList(),
    val accessToken: String,
    val refreshToken: String = "todo",
    val tokenType: String = "Bearer",
    val expiresIn: Int,
)