package pl.newbies.lecture.application

import pl.newbies.lecture.application.model.*
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
            theme = lecture.theme.let {
                ThemeResponse(
                    primaryColor = it.primaryColor,
                    secondaryColor = it.secondaryColor,
                    image = it.image,
                )
            },
            createDate = lecture.createDate,
            updateDate = lecture.updateDate,
        )
}