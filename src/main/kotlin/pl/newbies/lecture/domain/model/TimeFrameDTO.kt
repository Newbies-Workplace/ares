package pl.newbies.lecture.domain.model

import kotlinx.datetime.Instant

data class TimeFrameDTO(
    var startDate: Instant,

    var finishDate: Instant? = null,
)