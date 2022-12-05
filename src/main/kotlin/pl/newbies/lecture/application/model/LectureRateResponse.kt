package pl.newbies.lecture.application.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class LectureRateResponse(
    val id: String,
    val lectureId: String,

    val topicRate: Int,
    val presentationRate: Int,
    val opinion: String?,

    val createDate: Instant,
)