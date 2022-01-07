package pl.newbies.tag.domain.service

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.tag.domain.model.FollowedTag
import pl.newbies.tag.domain.model.Tag
import pl.newbies.tag.infrastructure.repository.*
import pl.newbies.user.infrastructure.repository.UserDAO
import pl.newbies.user.infrastructure.repository.Users
import java.util.*

class TagService {

    fun putFollowedTags(
        userId: String,
        tags: List<Tag>,
    ): List<FollowedTag> =
        transaction {
            tags.map {
                FollowedTagDAO.new(UUID.randomUUID().toString()) {
                    user = UserDAO(EntityID(userId, Users))
                    tag = TagDAO(EntityID(it.id, Tags))
                }.toFollowedTag()
            }
        }
}