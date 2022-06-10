package pl.newbies.lecture.application.model

import kotlinx.serialization.Serializable

@Serializable
class LectureFilter(
    val eventId: String,
)