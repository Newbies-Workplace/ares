package pl.newbies.auth.infrastructure

import pl.newbies.auth.domain.model.RefreshToken

interface RefreshTokenRepository {

    fun save(token: RefreshToken): RefreshToken

    fun findByRefreshToken(token: String): RefreshToken?
}