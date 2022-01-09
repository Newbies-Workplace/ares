package pl.newbies.lecture.application.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.valiktor.functions.isNotBlank
import org.valiktor.functions.isPositiveOrZero
import org.valiktor.validate
import pl.newbies.common.validator.distinct
import pl.newbies.tag.application.model.TagRequest

@Serializable
data class LectureRequest(
    val title: String,
    val subtitle: String?,
    val timeFrame: TimeFrameRequest,
    val address: AddressRequest?,
    val tags: List<TagRequest> = emptyList(),
) {

    init {
        validate(this) {
            validate(LectureRequest::title).isNotBlank()
            validate(LectureRequest::tags).distinct()
        }
    }
}

@Serializable
data class TimeFrameRequest(
    val startDate: Instant,
    val finishDate: Instant?,
)

@Serializable
data class AddressRequest(
    val city: String,
    val place: String,
    val coordinates: CoordinatesRequest?,
) {

    init {
        validate(this) {
            validate(AddressRequest::city).isNotBlank()
            validate(AddressRequest::place).isNotBlank()
        }
    }
}

@Serializable
data class CoordinatesRequest(
    val latitude: Double,
    val longitude: Double,
) {

    init {
        validate(this) {
            validate(CoordinatesRequest::latitude).isPositiveOrZero()
            validate(CoordinatesRequest::longitude).isPositiveOrZero()
        }
    }
}