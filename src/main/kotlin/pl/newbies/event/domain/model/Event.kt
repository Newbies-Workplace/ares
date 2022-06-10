package pl.newbies.event.domain.model

import kotlinx.datetime.Instant
import pl.newbies.tag.domain.model.Tag

data class Event(
    val id: String,
    var title: String,
    var subtitle: String? = null,
    val timeFrame: TimeFrameDTO,
    var address: AddressDTO? = null,
    var description: String?,

    val authorId: String,
    var tags: MutableList<Tag> = mutableListOf(),

    val vanityUrl: String,
    val theme: ThemeDTO = ThemeDTO(),
    val visibility: Visibility,

    val createDate: Instant,
    var updateDate: Instant,
) {
    enum class Visibility {
        PUBLIC,
        INVISIBLE,
        PRIVATE,
    }
}