package pl.newbies.auth.domain.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.auth.application.model.AuthResponse
import pl.newbies.auth.application.model.PropertiesResponse
import pl.newbies.auth.domain.model.RefreshToken
import pl.newbies.auth.infrastructure.repository.RefreshTokenDAO
import pl.newbies.auth.infrastructure.repository.RefreshTokens
import pl.newbies.auth.infrastructure.repository.toRefreshToken
import pl.newbies.common.nanoId
import pl.newbies.user.application.UserConverter
import pl.newbies.user.domain.model.User
import java.util.*

class AuthService(
    private val jwtSecret: String,
    private val jwtIssuer: String,
    private val userConverter: UserConverter,
) {

    fun generateResponse(user: User, previousToken: RefreshToken?): AuthResponse {
        val now = Clock.System.now()
        val expiresAt = Clock.System.now().plus(EXPIRE_IN_SECONDS, DateTimeUnit.SECOND)

        val accessToken = JWT.create()
            .withSubject(user.nickname)
            .withIssuer(jwtIssuer)
            .withAudience()
            .withClaim("roles", listOf<String>())
            .withClaim("id", user.id)
            .withClaim("nickname", user.nickname)
            .withExpiresAt(Date.from(expiresAt.toJavaInstant()))
            .withNotBefore(Date.from(now.toJavaInstant()))
            .withIssuedAt(Date.from(now.toJavaInstant()))
            .sign(Algorithm.HMAC256(jwtSecret))

        val token = transaction {
            previousToken?.let {
                RefreshTokenDAO[it.token].apply {
                    isUsed = true
                }
            }

            RefreshTokenDAO.new {
                this.userId = user.id
                this.family = previousToken?.family ?: nanoId()
                this.isUsed = false

                this.dateExpired = previousToken?.dateExpired
                    ?: now.plus(REFRESH_TOKEN_EXPIRE_IN_DAYS, DateTimeUnit.DAY, TimeZone.UTC)
                this.dateCreated = now
            }.toRefreshToken()
        }

        return AuthResponse(
            username = user.nickname,
            accessToken = accessToken,
            refreshToken = token.token,
            expiresIn = EXPIRE_IN_SECONDS,
            user = userConverter.convert(user),
            properties = PropertiesResponse(
                isInitialized = user.createDate != user.updateDate,
            )
        )
    }

    fun deleteTokenFamily(token: RefreshToken) {
        transaction {
            RefreshTokens.deleteWhere { RefreshTokens.family eq token.family }
        }
    }

    private companion object {
        const val EXPIRE_IN_SECONDS = 3600L
        const val REFRESH_TOKEN_EXPIRE_IN_DAYS = 30L
    }
}