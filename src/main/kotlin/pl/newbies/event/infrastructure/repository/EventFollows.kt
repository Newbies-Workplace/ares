package pl.newbies.event.infrastructure.repository

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import pl.newbies.event.domain.model.EventFollow
import pl.newbies.plugins.StringNanoIdEntity
import pl.newbies.plugins.StringNanoIdEntityClass
import pl.newbies.plugins.StringNanoIdTable
import pl.newbies.user.infrastructure.repository.UserDAO
import pl.newbies.user.infrastructure.repository.Users
import pl.newbies.user.infrastructure.repository.toUser

object EventFollows : StringNanoIdTable() {
    val event = reference("event", Events)
    val user = reference("user", Users)

    val followDate = timestamp("followDate")

    init {
        uniqueIndex(event, user)
    }
}

class EventFollowDAO(id: EntityID<String>) : StringNanoIdEntity(id) {
    companion object : StringNanoIdEntityClass<EventFollowDAO>(EventFollows)

    var event by EventDAO referencedOn EventFollows.event
    var user by UserDAO referencedOn EventFollows.user

    var followDate by EventFollows.followDate
}

fun EventFollowDAO.toEventFollow() =
    EventFollow(
        user = user.toUser(),
        event = event.toEvent(),
        followDate = followDate
    )