package pl.newbies.event.application

import pl.newbies.event.application.model.*
import pl.newbies.event.domain.model.Event
import pl.newbies.tag.application.TagConverter

class EventConverter(
    private val tagConverter: TagConverter,
) {

    fun convert(event: Event): EventResponse =
        EventResponse(
            id = event.id,
            authorId = event.authorId,
            title = event.title,
            subtitle = event.subtitle,
            timeFrame = event.timeFrame.let { timeFrame ->
                TimeFrameResponse(
                    startDate = timeFrame.startDate,
                    finishDate = timeFrame.finishDate
                )
            },
            address = event.address?.let { address ->
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
            tags = event.tags.map {
                tagConverter.convert(it)
            },
            theme = event.theme.let {
                ThemeResponse(
                    primaryColor = it.primaryColor,
                    secondaryColor = it.secondaryColor,
                    image = it.image,
                )
            },
            createDate = event.createDate,
            updateDate = event.updateDate,
        )
}