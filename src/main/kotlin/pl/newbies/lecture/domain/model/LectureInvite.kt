package pl.newbies.lecture.domain.model

import kotlinx.datetime.Instant

data class LectureInvite(
    val id: String,
    val lectureId: String,
    val name: String,
    val createDate: Instant,
)