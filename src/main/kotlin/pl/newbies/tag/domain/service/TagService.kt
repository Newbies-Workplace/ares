package pl.newbies.tag.domain.service

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.common.DuplicateException
import pl.newbies.tag.domain.model.FollowedTag
import pl.newbies.tag.domain.model.Tag
import pl.newbies.tag.infrastructure.repository.*
import pl.newbies.user.infrastructure.repository.UserDAO

class TagService {

    fun createTag(name: String): Tag = transaction {
        val count = TagDAO.count(Tags.name eq name)
        if (count != 0L) {
            throw DuplicateException("Tag with name $name already exists")
        }

        TagDAO.new {
            this.name = name
        }.toTag()
    }

    fun putFollowedTags(
        userId: String,
        tags: List<Tag>,
    ): List<FollowedTag> = transaction {
        tags.map {
            FollowedTagDAO.new {
                user = UserDAO[userId]
                tag = TagDAO[it.id]
            }.toFollowedTag()
        }
    }

    fun removeFollowedTags(
        userId: String,
        ids: List<String>,
    ) = transaction {
        FollowedTagDAO.find { (FollowedTags.tag inList ids) and (FollowedTags.user eq userId) }
            .forEach { it.delete() }
    }
}