package pl.newbies.event.domain.model

import kotlinx.datetime.Instant
import pl.newbies.user.domain.model.User

data class EventFollow(
    val event: Event,
    val user: User,

    val followDate: Instant,
)