package pl.newbies.lecture.infrastructure.repository

import kotlinx.datetime.Clock
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import pl.newbies.lecture.domain.model.AddressDTO
import pl.newbies.lecture.domain.model.CoordinatesDTO
import pl.newbies.lecture.domain.model.Lecture
import pl.newbies.lecture.domain.model.TimeFrameDTO
import pl.newbies.plugins.StringUUIDEntity
import pl.newbies.plugins.StringUUIDEntityClass
import pl.newbies.plugins.StringUUIDTable
import pl.newbies.tag.infrastructure.repository.TagDAO
import pl.newbies.tag.infrastructure.repository.Tags
import pl.newbies.user.infrastructure.repository.UserDAO
import pl.newbies.user.infrastructure.repository.Users

object Lectures : StringUUIDTable() {
    val title = varchar("title", 100)
    val subtitle = varchar("subtitle", 100).nullable()
    val author = reference("author", Users)

    val startDate = timestamp("startDate")
    val finishDate = timestamp("finishDate").nullable()

    val city = varchar("city", 50).nullable()
    val place = varchar("place", 50).nullable()
    val latitude = double("latitude").nullable()
    val longitude = double("longitude").nullable()


    val createDate = timestamp("createDate")
    val updateDate = timestamp("updateDate")
}

object LectureTags : Table() {
    val lecture = reference("lecture", Lectures)
    val tag = reference("tag", Tags)

    override val primaryKey: PrimaryKey =
        PrimaryKey(lecture, tag, name = "id")
}

class LectureDAO(id: EntityID<String>) : StringUUIDEntity(id) {
    companion object : StringUUIDEntityClass<LectureDAO>(Lectures)

    var title by Lectures.title
    var subtitle by Lectures.subtitle
    var author by UserDAO referencedOn Lectures.author

    var startDate by Lectures.startDate
    var finishDate by Lectures.finishDate

    var city by Lectures.city
    var place by Lectures.place
    var latitude by Lectures.latitude
    var longitude by Lectures.longitude

    var tags by TagDAO via LectureTags

    var createDate by Lectures.createDate
    var updateDate by Lectures.updateDate
}

fun LectureDAO.toLecture() = Lecture(
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
    createDate = createDate,
    updateDate = updateDate,
)