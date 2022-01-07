package pl.newbies.tag.infrastructure.repository

import org.jetbrains.exposed.dao.id.EntityID
import pl.newbies.plugins.StringUUIDEntity
import pl.newbies.plugins.StringUUIDEntityClass
import pl.newbies.plugins.StringUUIDTable
import pl.newbies.tag.domain.model.FollowedTag
import pl.newbies.tag.domain.model.Tag
import pl.newbies.user.infrastructure.repository.UserDAO
import pl.newbies.user.infrastructure.repository.Users
import pl.newbies.user.infrastructure.repository.toUser

object Tags : StringUUIDTable() {
    val name = varchar("name", length = 50)
}

class TagDAO(id: EntityID<String>) : StringUUIDEntity(id) {
    companion object : StringUUIDEntityClass<TagDAO>(Tags)

    val name by Tags.name
}

object FollowedTags : StringUUIDTable() {
    val user = reference("user", Users)
    val tag = reference("tag", Tags)
}

class FollowedTagDAO(id: EntityID<String>) : StringUUIDEntity(id) {
    companion object : StringUUIDEntityClass<FollowedTagDAO>(FollowedTags)

    var user by UserDAO referencedOn FollowedTags.user

    var tag by TagDAO referencedOn FollowedTags.tag
}

fun TagDAO.toTag() = Tag(
    id = id.value,
    name = name,
)

fun FollowedTagDAO.toFollowedTag() = FollowedTag(
    id = id.value,
    user = user.toUser(),
    tag = tag.toTag()
)