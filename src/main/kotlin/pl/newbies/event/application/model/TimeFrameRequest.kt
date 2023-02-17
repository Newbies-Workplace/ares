package pl.newbies.event.application.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class TimeFrameRequest(
    val startDate: Instant,
    val finishDate: Instant,
)