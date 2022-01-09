package pl.newbies.lecture.application.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import pl.newbies.tag.application.model.TagResponse

@Serializable
data class LectureResponse(
    val id: String,
    val authorId: String,
    val title: String,
    val subtitle: String?,
    val timeFrame: TimeFrameResponse,
    val address: AddressResponse?,
    val tags: List<TagResponse>,
    val createDate: Instant,
)

@Serializable
data class TimeFrameResponse(
    val startDate: Instant,
    val finishDate: Instant?,
)

@Serializable
data class AddressResponse(
    val city: String,
    val place: String,
    val coordinates: CoordinatesResponse?,
)

@Serializable
data class CoordinatesResponse(
    val latitude: Double,
    val longitude: Double,
)