package pl.newbies.tag.domain.model

import pl.newbies.user.domain.model.User
import java.util.*

data class FollowedTag(
    var id: String = UUID.randomUUID().toString(),

    var user: User,

    var tag: Tag,
)