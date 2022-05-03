package pl.newbies.auth.domain.model

import kotlinx.datetime.Instant

data class RefreshToken(
    var token: String,
    var family: String,
    var userId: String,
    var isUsed: Boolean,
    var dateExpired: Instant,
    var dateCreated: Instant,
)