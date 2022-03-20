package pl.newbies.lecture.domain.model

import kotlinx.datetime.Instant
import pl.newbies.tag.domain.model.Tag
import java.util.*

data class Lecture(
    val id: String = UUID.randomUUID().toString(),
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