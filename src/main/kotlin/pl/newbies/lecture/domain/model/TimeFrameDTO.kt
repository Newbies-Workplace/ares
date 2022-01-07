package pl.newbies.lecture.domain.model

import java.time.Instant

data class TimeFrameDTO(
    var startDate: Instant,

    var finishDate: Instant? = null,
)