package pl.newbies.event.domain.model

import kotlinx.datetime.Instant
import pl.newbies.tag.domain.model.Tag

data class Event(
    val id: String,
    var title: String,
    var subtitle: String? = null,
    val timeFrame: TimeFrameDTO,
    var address: AddressDTO? = null,

    val authorId: String,
    var tags: MutableList<Tag> = mutableListOf(),

    val theme: ThemeDTO = ThemeDTO(),

    val createDate: Instant,
    var updateDate: Instant,
)