package pl.newbies.lecture.application.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import pl.newbies.event.application.model.TimeFrameResponse
import pl.newbies.user.application.model.UserResponse

@Serializable
data class LectureResponse(
    val id: String,
    val title: String,
    val description: String?,
    val timeFrame: TimeFrameResponse,
    val createDate: Instant,
    val updateDate: Instant,
    val author: UserResponse,
    val speakers: List<UserResponse>,
)