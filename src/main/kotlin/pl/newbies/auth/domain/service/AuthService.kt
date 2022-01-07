package pl.newbies.auth.domain.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import pl.newbies.auth.domain.model.AuthResponse
import pl.newbies.user.domain.model.User
import java.util.*

class AuthService(
    private val jwtSecret: String,
    private val jwtIssuer: String,
) {

    //todo refresh token
    fun generateResponse(user: User): AuthResponse {
        val now = Date()
        val expiresAt = Date(now.time + (EXPIRE_IN_SECONDS * 1000))

        val accessToken = JWT.create()
            .withSubject(user.nickname)
            .withIssuer(jwtIssuer)
            .withAudience()
            .withClaim("roles", listOf<String>())
            .withClaim("id", user.id)
            .withClaim("nickname", user.nickname)
            .withExpiresAt(expiresAt)
            .withNotBefore(now)
            .withIssuedAt(now)
            .sign(Algorithm.HMAC256(jwtSecret))

        return AuthResponse(
            username = user.nickname,
            accessToken = accessToken,
            refreshToken = "todo",
            expiresIn = EXPIRE_IN_SECONDS,
        )
    }

    private companion object {
        const val EXPIRE_IN_SECONDS = 3600
    }
}