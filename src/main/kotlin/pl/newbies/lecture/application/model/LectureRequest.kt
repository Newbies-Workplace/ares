package pl.newbies.lecture.application.model

import kotlinx.serialization.Serializable
import org.valiktor.functions.hasSize
import org.valiktor.functions.isNotBlank
import org.valiktor.validate
import pl.newbies.common.validator.distinct
import pl.newbies.common.validator.maxLines
import pl.newbies.event.application.model.TimeFrameRequest

@Serializable
data class LectureRequest(
    val title: String,
    val description: String? = null,
    val timeFrame: TimeFrameRequest,
    val speakerIds: List<String>,
) {

    init {
        validate(this) {
            validate(LectureRequest::title)
                .isNotBlank()
                .hasSize(min = 3, max = 100)
                .maxLines(1)
            validate(LectureRequest::description)
                .isNotBlank()
                .hasSize(max = 5_000)
            validate(LectureRequest::speakerIds)
                .hasSize(max = 2)
                .distinct()
        }
    }
}