package pl.newbies.event.infrastructure.repository

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import pl.newbies.event.domain.model.*
import pl.newbies.plugins.StringUUIDEntity
import pl.newbies.plugins.StringUUIDEntityClass
import pl.newbies.plugins.StringUUIDTable
import pl.newbies.tag.infrastructure.repository.TagDAO
import pl.newbies.tag.infrastructure.repository.Tags
import pl.newbies.user.infrastructure.repository.UserDAO
import pl.newbies.user.infrastructure.repository.Users
import pl.newbies.user.infrastructure.repository.toUser

object Events : StringUUIDTable() {
    val title = varchar("title", length = 100, collate = "utf8_general_ci")
    val subtitle = varchar("subtitle", length = 100, collate = "utf8_general_ci").nullable()
    val author = reference("author", Users)

    val startDate = timestamp("startDate")
    val finishDate = timestamp("finishDate").nullable()

    val city = varchar("city", length = 50, collate = "utf8_general_ci").nullable()
    val place = varchar("place", length = 50, collate = "utf8_general_ci").nullable()
    val latitude = double("latitude").nullable()
    val longitude = double("longitude").nullable()

    val primaryColor = varchar("primaryColor", length = 7).nullable()
    val secondaryColor = varchar("secondaryColor", length = 7).nullable()
    val image = varchar("image", length = 100).nullable()

    val createDate = timestamp("createDate")
    val updateDate = timestamp("updateDate")
}

object EventTags : Table() {
    val event = reference("event", Events)
    val tag = reference("tag", Tags)

    override val primaryKey: PrimaryKey =
        PrimaryKey(event, tag, name = "id")
}

object EventFollows : StringUUIDTable() {
    val event = reference("event", Events)
    val user = reference("user", Users)

    val followDate = timestamp("followDate")

    init {
        uniqueIndex(event, user)
    }
}

class EventFollowDAO(id: EntityID<String>) : StringUUIDEntity(id) {
    companion object : StringUUIDEntityClass<EventFollowDAO>(EventFollows)

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

class EventDAO(id: EntityID<String>) : StringUUIDEntity(id) {
    companion object : StringUUIDEntityClass<EventDAO>(Events)

    var title by Events.title
    var subtitle by Events.subtitle
    var author by UserDAO referencedOn Events.author

    var startDate by Events.startDate
    var finishDate by Events.finishDate

    var city by Events.city
    var place by Events.place
    var latitude by Events.latitude
    var longitude by Events.longitude

    var tags by TagDAO via EventTags

    var primaryColor by Events.primaryColor
    var secondaryColor by Events.secondaryColor
    var image by Events.image

    var createDate by Events.createDate
    var updateDate by Events.updateDate
}

fun EventDAO.toEvent() = Event(
    id = id.value,
    title = title,
    subtitle = subtitle,
    authorId = author.id.value,
    timeFrame = TimeFrameDTO(
        startDate = startDate,
        finishDate = finishDate,
    ),
    address = if (city != null && place != null) {
        AddressDTO(
            city = city!!,
            place = place!!,
            coordinates = if (latitude != null && longitude != null) {
                CoordinatesDTO(
                    latitude = latitude!!,
                    longitude = longitude!!,
                )
            } else null
        )
    } else null,
    theme = ThemeDTO(
        primaryColor = primaryColor,
        secondaryColor = secondaryColor,
        image = image,
    ),
    createDate = createDate,
    updateDate = updateDate,
)