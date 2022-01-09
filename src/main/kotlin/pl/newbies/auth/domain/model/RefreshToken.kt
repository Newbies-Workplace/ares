package pl.newbies.auth.domain.model

import kotlinx.datetime.Instant

data class RefreshToken(
    var token: String,
    var userId: String,
    var dateCreated: Instant? = null,
)