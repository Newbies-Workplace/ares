package pl.newbies.lecture.domain.service

import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.lecture.application.model.LectureRequest
import pl.newbies.lecture.application.model.LectureThemeRequest
import pl.newbies.lecture.domain.model.Lecture
import pl.newbies.lecture.domain.model.LectureFollow
import pl.newbies.lecture.infrastructure.repository.*
import pl.newbies.storage.domain.model.FileResource
import pl.newbies.tag.infrastructure.repository.TagDAO
import pl.newbies.tag.infrastructure.repository.Tags
import pl.newbies.user.domain.model.User
import pl.newbies.user.infrastructure.repository.UserDAO
import java.util.*

class LectureService {

    fun createLecture(request: LectureRequest, authorId: String): Lecture = transaction {
        val tags = TagDAO.find { Tags.id inList request.tags.map { it.id } }
            .toMutableList()

        val now = Clock.System.now()

        LectureDAO.new(UUID.randomUUID().toString()) {
            this.title = request.title
            this.subtitle = request.subtitle
            this.author = UserDAO[authorId]
            request.timeFrame.let { frame ->
                this.startDate = frame.startDate
                this.finishDate = frame.finishDate
            }
            request.address?.let { address ->
                this.city = address.city
                this.place = address.place
                address.coordinates?.let { coordinates ->
                    this.latitude = coordinates.latitude
                    this.longitude = coordinates.longitude
                }
            }
            this.tags = SizedCollection(tags)

            this.createDate = now
            this.updateDate = now
        }.toLecture()
    }

    fun updateLecture(lecture: Lecture, request: LectureRequest): Lecture = transaction {
        val tags = TagDAO.find { Tags.id inList request.tags.map { it.id } }
            .toMutableList()

        LectureDAO[lecture.id]
            .apply {
                this.title = request.title
                this.subtitle = request.subtitle
                request.timeFrame.let { frame ->
                    this.startDate = frame.startDate
                    this.finishDate = frame.finishDate
                }
                request.address.let { address ->
                    this.city = address?.city
                    this.place = address?.place
                    address?.coordinates.let { coordinates ->
                        this.latitude = coordinates?.latitude
                        this.longitude = coordinates?.longitude
                    }
                }
                this.tags = SizedCollection(tags)

                this.updateDate = Clock.System.now()
            }
            .toLecture()
    }

    fun updateTheme(lecture: Lecture, request: LectureThemeRequest): Lecture = transaction {
        LectureDAO[lecture.id]
            .apply {
                this.primaryColor = request.primaryColor
                this.secondaryColor = request.secondaryColor
            }
            .toLecture()
    }

    fun updateThemeImage(lecture: Lecture, fileResource: FileResource?): Lecture = transaction {
        LectureDAO[lecture.id]
            .apply {
                this.image = fileResource?.pathWithName
            }
            .toLecture()
    }

    fun deleteLecture(lecture: Lecture) = transaction {
        LectureDAO[lecture.id].delete()
    }

    fun followLecture(user: User, lecture: Lecture): LectureFollow =
        transaction {
            LectureFollowDAO.find { (LectureFollows.user eq user.id) and (LectureFollows.lecture eq lecture.id) }
                .firstOrNull()
                ?.toLectureFollow()
                ?.let { return@transaction it }

            LectureFollowDAO.new(UUID.randomUUID().toString()) {
                this.user = UserDAO[user.id]
                this.lecture = LectureDAO[lecture.id]

                this.followDate = Clock.System.now()
            }.toLectureFollow()
        }

    fun unfollowLecture(user: User, lecture: Lecture) {
        transaction {
            LectureFollowDAO.find { (LectureFollows.user eq user.id) and (LectureFollows.lecture eq lecture.id) }
                .firstOrNull()
                ?.delete()
        }
    }

    fun getThemeImageFileResource(lecture: Lecture): FileResource? {
        if (lecture.theme.image == null) return null

        val storagePath = "lectures/${lecture.id}/"
        val fileName = lecture.theme.image.substringAfter(storagePath)

        return FileResource(
            storagePath = storagePath,
            name = fileName
        )
    }
}