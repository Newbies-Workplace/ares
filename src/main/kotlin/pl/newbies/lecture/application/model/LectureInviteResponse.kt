package pl.newbies.lecture.application.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class LectureInviteResponse(
    val id: String,
    val lectureId: String,
    val name: String,
    val createDate: Instant,
)