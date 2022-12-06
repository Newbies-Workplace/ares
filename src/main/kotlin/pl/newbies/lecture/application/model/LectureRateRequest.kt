package pl.newbies.lecture.application.model

import kotlinx.serialization.Serializable
import org.valiktor.functions.hasSize
import org.valiktor.functions.isBetween
import org.valiktor.functions.isNotBlank
import org.valiktor.validate

@Serializable
data class LectureRateRequest(
    val topicRate: Int,
    val presentationRate: Int,
    val opinion: String?,
) {

    init {
        validate(this) {
            validate(LectureRateRequest::topicRate)
                .isBetween(1, 5)
            validate(LectureRateRequest::presentationRate)
                .isBetween(1, 5)
            validate(LectureRateRequest::opinion)
                .isNotBlank()
                .hasSize(max = 1_000)
        }
    }
}