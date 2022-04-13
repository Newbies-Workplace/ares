package pl.newbies.event.application.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.valiktor.functions.hasSize
import org.valiktor.functions.isGreaterThanOrEqualTo
import org.valiktor.functions.isLessThanOrEqualTo
import org.valiktor.functions.isNotBlank
import org.valiktor.validate
import pl.newbies.common.validator.distinct
import pl.newbies.common.validator.oneLine
import pl.newbies.tag.application.model.TagRequest

@Serializable
data class EventRequest(
    val title: String,
    val subtitle: String?,
    val timeFrame: TimeFrameRequest,
    val address: AddressRequest?,
    val tags: List<TagRequest> = emptyList(),
) {

    init {
        validate(this) {
            validate(EventRequest::title)
                .isNotBlank()
                .hasSize(min = 10, max = 100)
                .oneLine()
            validate(EventRequest::subtitle)
                .isNotBlank()
                .hasSize(max = 100)
                .oneLine()
            validate(EventRequest::tags)
                .distinct()
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
            validate(AddressRequest::city)
                .isNotBlank()
                .hasSize(max = 50)
                .oneLine()
            validate(AddressRequest::place)
                .isNotBlank()
                .hasSize(max = 50)
                .oneLine()
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
            validate(CoordinatesRequest::latitude)
                .isGreaterThanOrEqualTo(-90.0)
                .isLessThanOrEqualTo(90.0)
            validate(CoordinatesRequest::longitude)
                .isGreaterThanOrEqualTo(-180.0)
                .isLessThanOrEqualTo(180.0)
        }
    }
}