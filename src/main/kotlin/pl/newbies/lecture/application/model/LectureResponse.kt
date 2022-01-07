package pl.newbies.lecture.application.model

import pl.newbies.tag.application.model.TagResponse
import java.time.Instant

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

data class TimeFrameResponse(
    val startDate: Instant,
    val finishDate: Instant?,
)

data class AddressResponse(
    val city: String,
    val place: String,
    val coordinates: CoordinatesResponse?,
)

data class CoordinatesResponse(
    val latitude: Double,
    val longitude: Double,
)