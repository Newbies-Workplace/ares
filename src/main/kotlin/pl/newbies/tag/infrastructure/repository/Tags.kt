package pl.newbies.tag.infrastructure.repository

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.ReferenceOption
import pl.newbies.plugins.StringNanoIdEntity
import pl.newbies.plugins.StringNanoIdEntityClass
import pl.newbies.plugins.StringNanoIdTable
import pl.newbies.tag.domain.model.FollowedTag
import pl.newbies.tag.domain.model.Tag
import pl.newbies.user.infrastructure.repository.UserDAO
import pl.newbies.user.infrastructure.repository.Users
import pl.newbies.user.infrastructure.repository.toUser

object Tags : StringNanoIdTable() {
    val name = varchar("name", length = 50, collate = "utf8_general_ci").uniqueIndex()
}

class TagDAO(id: EntityID<String>) : StringNanoIdEntity(id) {
    companion object : StringNanoIdEntityClass<TagDAO>(Tags)

    var name by Tags.name
}

object FollowedTags : StringNanoIdTable() {
    val user = reference("user", Users)
    val tag = reference("tag", Tags, onDelete = ReferenceOption.CASCADE)
}

class FollowedTagDAO(id: EntityID<String>) : StringNanoIdEntity(id) {
    companion object : StringNanoIdEntityClass<FollowedTagDAO>(FollowedTags)

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