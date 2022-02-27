package pl.newbies.lecture.infrastructure.repository

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import pl.newbies.lecture.domain.model.*
import pl.newbies.plugins.StringUUIDEntity
import pl.newbies.plugins.StringUUIDEntityClass
import pl.newbies.plugins.StringUUIDTable
import pl.newbies.tag.infrastructure.repository.TagDAO
import pl.newbies.tag.infrastructure.repository.Tags
import pl.newbies.user.infrastructure.repository.UserDAO
import pl.newbies.user.infrastructure.repository.Users
import pl.newbies.user.infrastructure.repository.toUser

object Lectures : StringUUIDTable() {
    val title = varchar("title", length = 100, collate = "utf8_general_ci")
    val subtitle = varchar("subtitle", length = 100, collate = "utf8_general_ci").nullable()
    val author = reference("author", Users)

    val startDate = timestamp("startDate")
    val finishDate = timestamp("finishDate").nullable()

    val city = varchar("city", length = 50, collate = "utf8_general_ci").nullable()
    val place = varchar("place", length = 50, collate = "utf8_general_ci").nullable()
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

object LectureFollows : StringUUIDTable() {
    val lecture = reference("lecture", Lectures)
    val user = reference("user", Users)

    val followDate = timestamp("followDate")

    init {
        uniqueIndex(lecture, user)
    }
}

class LectureFollowDAO(id: EntityID<String>) : StringUUIDEntity(id) {
    companion object : StringUUIDEntityClass<LectureFollowDAO>(LectureFollows)

    var lecture by LectureDAO referencedOn LectureFollows.lecture
    var user by UserDAO referencedOn LectureFollows.user

    var followDate by LectureFollows.followDate
}

fun LectureFollowDAO.toLectureFollow() =
    LectureFollow(
        user = user.toUser(),
        lecture = lecture.toLecture(),
        followDate = followDate
    )

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