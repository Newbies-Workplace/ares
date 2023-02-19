package pl.newbies.lecture.infrastructure.repository

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import pl.newbies.event.domain.model.TimeFrameDTO
import pl.newbies.event.infrastructure.repository.EventDAO
import pl.newbies.event.infrastructure.repository.Events
import pl.newbies.lecture.domain.model.Lecture
import pl.newbies.lecture.domain.model.LectureInvite
import pl.newbies.plugins.StringNanoIdEntity
import pl.newbies.plugins.StringNanoIdEntityClass
import pl.newbies.plugins.StringNanoIdTable
import pl.newbies.user.infrastructure.repository.UserDAO
import pl.newbies.user.infrastructure.repository.Users
import pl.newbies.user.infrastructure.repository.toUser

object Lectures : StringNanoIdTable() {
    val event = reference("event", Events)

    val title = varchar("title", length = 100, collate = "utf8mb4_unicode_ci")
    val description = text("description", collate = "utf8mb4_unicode_ci").nullable()
    val author = reference("author", Users)

    val startDate = timestamp("startDate")
    val finishDate = timestamp("finishDate")

    val createDate = timestamp("createDate")
    val updateDate = timestamp("updateDate")
}

object LectureSpeakers : Table() {
    val lecture = reference("lecture", Lectures, onDelete = ReferenceOption.CASCADE)
    val user = reference("user", Users)

    override val primaryKey: PrimaryKey =
        PrimaryKey(lecture, user, name = "id")
}

object LectureInvites : StringNanoIdTable() {
    val lecture = reference("lecture", Lectures, onDelete = ReferenceOption.CASCADE)

    val name = varchar("name", length = 50, collate = "utf8mb4_unicode_ci")

    val createDate = timestamp("createDate")
}

class LectureDAO(id: EntityID<String>) : StringNanoIdEntity(id) {
    companion object : StringNanoIdEntityClass<LectureDAO>(Lectures)

    var event by EventDAO referencedOn Lectures.event

    var title by Lectures.title
    var description by Lectures.description

    var author by UserDAO referencedOn Lectures.author
    var speakers by UserDAO via LectureSpeakers
    val invites by LectureInviteDAO referrersOn LectureInvites.lecture

    var startDate by Lectures.startDate
    var finishDate by Lectures.finishDate

    var createDate by Lectures.createDate
    var updateDate by Lectures.updateDate
}

class LectureInviteDAO(id: EntityID<String>) : StringNanoIdEntity(id) {
    companion object : StringNanoIdEntityClass<LectureInviteDAO>(LectureInvites)

    var lecture by LectureDAO referencedOn LectureInvites.lecture

    var name by LectureInvites.name

    var createDate by LectureInvites.createDate
}

fun LectureDAO.toLecture() = Lecture(
    id = id.value,
    eventId = event.id.value,
    title = title,
    description = description,
    author = author.toUser(),
    speakers = speakers.map { it.toUser() }.toMutableList(),
    timeFrame = TimeFrameDTO(
        startDate = startDate,
        finishDate = finishDate,
    ),
    createDate = createDate,
    updateDate = updateDate,
)

fun LectureInviteDAO.toInvite() = LectureInvite(
    id = id.value,
    lectureId = lecture.id.value,
    name = name,
    createDate = createDate,
)