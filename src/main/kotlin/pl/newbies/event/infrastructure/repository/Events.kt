package pl.newbies.event.infrastructure.repository

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import pl.newbies.event.domain.model.*
import pl.newbies.plugins.StringNanoIdEntity
import pl.newbies.plugins.StringNanoIdEntityClass
import pl.newbies.plugins.StringNanoIdTable
import pl.newbies.tag.infrastructure.repository.TagDAO
import pl.newbies.tag.infrastructure.repository.Tags
import pl.newbies.tag.infrastructure.repository.toTag
import pl.newbies.user.infrastructure.repository.UserDAO
import pl.newbies.user.infrastructure.repository.Users

object Events : StringNanoIdTable() {
    val title = varchar("title", length = 100, collate = "utf8mb4_unicode_ci")
    val subtitle = varchar("subtitle", length = 100, collate = "utf8mb4_unicode_ci").nullable()
    val author = reference("author", Users)

    val description = text("description", collate = "utf8mb4_unicode_ci").nullable()

    val vanityUrl = varchar("vanityUrl", length = 50, collate = "utf8_general_ci")

    val startDate = timestamp("startDate")
    val finishDate = timestamp("finishDate")

    val place = varchar("place", length = 100, collate = "utf8_general_ci").nullable()
    val latitude = double("latitude").nullable()
    val longitude = double("longitude").nullable()

    val primaryColor = varchar("primaryColor", length = 7).nullable()
    val image = varchar("image", length = 100).nullable()

    val visibility = enumerationByName("visibility", 20, Event.Visibility::class)

    val createDate = timestamp("createDate")
    val updateDate = timestamp("updateDate")
}

object EventTags : Table() {
    val event = reference("event", Events)
    val tag = reference("tag", Tags)

    override val primaryKey: PrimaryKey =
        PrimaryKey(event, tag, name = "id")
}

class EventDAO(id: EntityID<String>) : StringNanoIdEntity(id) {
    companion object : StringNanoIdEntityClass<EventDAO>(Events)

    var title by Events.title
    var subtitle by Events.subtitle
    var author by UserDAO referencedOn Events.author

    var description by Events.description

    var vanityUrl by Events.vanityUrl

    var startDate by Events.startDate
    var finishDate by Events.finishDate

    var place by Events.place
    var latitude by Events.latitude
    var longitude by Events.longitude

    var tags by TagDAO via EventTags

    var primaryColor by Events.primaryColor
    var image by Events.image

    var visibility by Events.visibility

    var createDate by Events.createDate
    var updateDate by Events.updateDate
}

fun EventDAO.toEvent() = Event(
    id = id.value,
    title = title,
    subtitle = subtitle,
    timeFrame = TimeFrameDTO(
        startDate = startDate,
        finishDate = finishDate,
    ),
    address = if (place != null) {
        AddressDTO(
            place = place!!,
            coordinates = if (latitude != null && longitude != null) {
                CoordinatesDTO(
                    latitude = latitude!!,
                    longitude = longitude!!,
                )
            } else null
        )
    } else null,
    description = description,
    authorId = author.id.value,
    tags = tags.map { it.toTag() }.toMutableList(),
    vanityUrl = vanityUrl,
    theme = ThemeDTO(
        primaryColor = primaryColor,
        image = image,
    ),
    visibility = visibility,
    createDate = createDate,
    updateDate = updateDate,
)