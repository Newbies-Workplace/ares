package pl.newbies.tag.domain.model

import pl.newbies.user.domain.model.User

data class FollowedTag(
    var id: String,
    var user: User,
    var tag: Tag,
)