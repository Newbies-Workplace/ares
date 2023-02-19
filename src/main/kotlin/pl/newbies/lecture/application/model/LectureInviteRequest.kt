package pl.newbies.lecture.application.model

import kotlinx.serialization.Serializable

@Serializable
data class LectureInviteRequest(
    val name: String,
)