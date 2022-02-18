package pl.newbies.lecture.domain.model

import kotlinx.datetime.Instant
import pl.newbies.user.domain.model.User

data class LectureFollow(
    val lecture: Lecture,
    val user: User,

    val followDate: Instant,
)