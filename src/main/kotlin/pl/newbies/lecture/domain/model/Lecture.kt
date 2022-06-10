package pl.newbies.lecture.domain.model

import kotlinx.datetime.Instant
import pl.newbies.event.domain.model.TimeFrameDTO
import pl.newbies.user.domain.model.User

data class Lecture(
    val id: String,
    val eventId: String,

    var title: String,
    var description: String?,
    val timeFrame: TimeFrameDTO,

    val author: User,
    val speakers: List<User>,

    val createDate: Instant,
    var updateDate: Instant,
)