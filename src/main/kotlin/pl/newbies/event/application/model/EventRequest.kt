package pl.newbies.event.application.model

import kotlinx.serialization.Serializable
import org.valiktor.functions.hasSize
import org.valiktor.functions.isGreaterThanOrEqualTo
import org.valiktor.functions.isLessThanOrEqualTo
import org.valiktor.functions.isNotBlank
import org.valiktor.validate
import pl.newbies.common.validator.distinct
import pl.newbies.common.validator.maxLines
import pl.newbies.tag.application.model.TagRequest

@Serializable
data class EventRequest(
    val title: String,
    val subtitle: String? = null,
    val description: String? = null,
    val timeFrame: TimeFrameRequest,
    val address: AddressRequest? = null,
    val tags: List<TagRequest> = emptyList(),
) {

    init {
        validate(this) {
            validate(EventRequest::title)
                .isNotBlank()
                .hasSize(min = 3, max = 100)
                .maxLines(1)
            validate(EventRequest::subtitle)
                .isNotBlank()
                .hasSize(max = 100)
                .maxLines(1)
            validate(EventRequest::description)
                .isNotBlank()
                .hasSize(max = 5_000)
            validate(EventRequest::tags)
                .distinct()
        }
    }
}

@Serializable
data class AddressRequest(
    val city: String,
    val place: String,
    val coordinates: CoordinatesRequest? = null,
) {

    init {
        validate(this) {
            validate(AddressRequest::city)
                .isNotBlank()
                .hasSize(max = 50)
                .maxLines(1)
            validate(AddressRequest::place)
                .isNotBlank()
                .hasSize(max = 100)
                .maxLines(5)
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