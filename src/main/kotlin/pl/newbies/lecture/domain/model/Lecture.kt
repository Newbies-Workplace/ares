package pl.newbies.lecture.domain.model

import pl.newbies.tag.domain.model.Tag
import java.time.Instant
import java.util.*

data class Lecture(
    var id: String = UUID.randomUUID().toString(),

    var title: String,

    var subtitle: String? = null,

    var authorId: String,

    var timeFrame: TimeFrameDTO,

    var address: AddressDTO? = null,

    var tags: MutableList<Tag> = mutableListOf()
) {

    lateinit var createDate: Instant
}