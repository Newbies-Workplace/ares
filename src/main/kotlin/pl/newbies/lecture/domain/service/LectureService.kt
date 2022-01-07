package pl.newbies.lecture.domain.service

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.lecture.domain.model.Lecture
import pl.newbies.lecture.application.model.LectureRequest
import pl.newbies.lecture.domain.LectureNotFoundException
import pl.newbies.lecture.infrastructure.repository.LectureDAO
import pl.newbies.lecture.infrastructure.repository.Lectures
import pl.newbies.lecture.infrastructure.repository.toLecture
import pl.newbies.tag.infrastructure.repository.TagDAO
import pl.newbies.tag.infrastructure.repository.Tags
import pl.newbies.user.infrastructure.repository.UserDAO
import pl.newbies.user.infrastructure.repository.Users
import java.util.*

class LectureService {

    fun createLecture(request: LectureRequest, authorId: String): Lecture = transaction {
        val tags = TagDAO.find { Tags.id inList request.tags.map { it.id } }
            .toMutableList()

        LectureDAO.new(UUID.randomUUID().toString()) {
            this.title = title
            this.subtitle = subtitle
            this.author = UserDAO(EntityID(authorId, Users))
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
        }.toLecture()
    }

    fun updateLecture(lecture: Lecture, request: LectureRequest): Lecture = transaction {
        val tags = TagDAO.find { Tags.id inList request.tags.map { it.id } }
            .toMutableList()

        LectureDAO.findById(lecture.id)
            ?.apply {
                this.title = title
                this.subtitle = subtitle
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
            }
            ?.toLecture()
            ?: throw LectureNotFoundException(lecture.id)
    }

    fun deleteLecture(lecture: Lecture) = transaction {
        LectureDAO(EntityID(lecture.id, Lectures))
            .delete()
    }
}