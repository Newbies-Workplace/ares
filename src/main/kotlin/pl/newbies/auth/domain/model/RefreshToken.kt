package pl.newbies.auth.domain.model

import java.time.Instant

data class RefreshToken(
    var id: String,
    var userId: String,
    var token: String,
    var revoked: Boolean,
    var dateCreated: Instant? = null
)