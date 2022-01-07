package pl.newbies.tag.application

import pl.newbies.tag.application.model.TagResponse
import pl.newbies.tag.domain.model.Tag

class TagConverter {

    fun convert(tag: Tag): TagResponse =
        TagResponse(
            id = tag.id,
            name = tag.name,
        )
}