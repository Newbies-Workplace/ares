package pl.newbies.lecture.domain.service

import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.lecture.application.model.LectureRequest
import pl.newbies.lecture.domain.LectureNotFoundException
import pl.newbies.lecture.domain.model.Lecture
import pl.newbies.lecture.infrastructure.repository.LectureDAO
import pl.newbies.lecture.infrastructure.repository.toLecture
import pl.newbies.tag.infrastructure.repository.TagDAO
import pl.newbies.tag.infrastructure.repository.Tags
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

        LectureDAO.findById(lecture.id)
            ?.apply {
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
            ?.toLecture()
            ?: throw LectureNotFoundException(lecture.id)
    }

    fun deleteLecture(lecture: Lecture) = transaction {
        LectureDAO[lecture.id].delete()
    }
}