package pl.newbies.lecture.application

import pl.newbies.lecture.application.model.AddressResponse
import pl.newbies.lecture.application.model.CoordinatesResponse
import pl.newbies.lecture.application.model.LectureResponse
import pl.newbies.lecture.application.model.TimeFrameResponse
import pl.newbies.lecture.domain.model.Lecture
import pl.newbies.tag.application.TagConverter

class LectureConverter(
    private val tagConverter: TagConverter,
) {

    fun convert(lecture: Lecture): LectureResponse =
        LectureResponse(
            id = lecture.id,
            authorId = lecture.authorId,
            title = lecture.title,
            subtitle = lecture.subtitle,
            timeFrame = lecture.timeFrame.let { timeFrame ->
                TimeFrameResponse(
                    startDate = timeFrame.startDate,
                    finishDate = timeFrame.finishDate
                )
            },
            address = lecture.address?.let { address ->
                AddressResponse(
                    city = address.city,
                    place = address.place,
                    coordinates = address.coordinates?.let { coordinates ->
                        CoordinatesResponse(
                            latitude = coordinates.latitude,
                            longitude = coordinates.longitude,
                        )
                    }
                )
            },
            tags = lecture.tags.map {
                tagConverter.convert(it)
            },
            createDate = lecture.createDate,
            updateDate = lecture.updateDate,
        )
}