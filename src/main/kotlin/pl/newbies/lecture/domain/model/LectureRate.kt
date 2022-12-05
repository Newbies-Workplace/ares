package pl.newbies.lecture.domain.model

import kotlinx.datetime.Instant

data class LectureRate(
    val id: String,
    val lectureId: String,

    val topicRate: Int,
    val presentationRate: Int,
    val opinion: String?,

    val createDate: Instant,
)