package pl.newbies.lecture.domain.service

import io.ktor.server.plugins.BadRequestException
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.common.NotFoundException
import pl.newbies.common.nanoId
import pl.newbies.event.domain.model.Event
import pl.newbies.event.infrastructure.repository.EventDAO
import pl.newbies.event.infrastructure.repository.toEvent
import pl.newbies.lecture.application.model.LectureFilter
import pl.newbies.lecture.application.model.LectureRequest
import pl.newbies.lecture.domain.model.Lecture
import pl.newbies.lecture.infrastructure.repository.LectureDAO
import pl.newbies.lecture.infrastructure.repository.Lectures
import pl.newbies.lecture.infrastructure.repository.toLecture
import pl.newbies.user.infrastructure.repository.UserDAO
import pl.newbies.user.infrastructure.repository.Users

class LectureService {

    fun createLecture(
        request: LectureRequest,
        event: Event,
        authorId: String,
    ): Lecture = transaction {
        assertLectureTimeFrameFitsEvent(
            lectureStartDate = request.timeFrame.startDate,
            lectureFinishDate = request.timeFrame.finishDate,
            eventStartDate = event.timeFrame.startDate,
            eventFinishDate = event.timeFrame.finishDate,
        )

        val now = Clock.System.now()

        val lecture = LectureDAO.new(nanoId()) {
            appendRequestFields(request)

            this.event = EventDAO[event.id]
            this.author = UserDAO[authorId]

            this.createDate = now
            this.updateDate = now
        }.load(LectureDAO::speakers)

        lecture.toLecture()
    }

    fun getLectures(filter: LectureFilter): List<Lecture> = transaction {
        LectureDAO.find { Lectures.event eq filter.eventId }
            .map { it.toLecture() }
    }

    fun updateLecture(
        lecture: Lecture,
        request: LectureRequest,
    ): Lecture = transaction {
        val event = EventDAO.findById(lecture.eventId)?.toEvent()
            ?: throw NotFoundException("Event with lectureId=${lecture.id} not found.")

        assertLectureTimeFrameFitsEvent(
            lectureStartDate = request.timeFrame.startDate,
            lectureFinishDate = request.timeFrame.finishDate,
            eventStartDate = event.timeFrame.startDate,
            eventFinishDate = event.timeFrame.finishDate,
        )

        LectureDAO[lecture.id].apply {
            appendRequestFields(request)

            this.updateDate = Clock.System.now()
        }.load(LectureDAO::speakers)
            .toLecture()
    }

    fun deleteLecture(lecture: Lecture) = transaction {
        LectureDAO[lecture.id].delete()
    }

    private fun LectureDAO.appendRequestFields(request: LectureRequest) {
        val speakers = UserDAO.find { Users.id inList request.speakerIds }

        this.speakers = speakers

        this.title = request.title.trim()
        this.description = request.description?.trim()
        this.startDate = request.timeFrame.startDate
        this.finishDate = request.timeFrame.finishDate
    }

    private fun assertLectureTimeFrameFitsEvent(
        lectureStartDate: Instant,
        lectureFinishDate: Instant?,
        eventStartDate: Instant,
        eventFinishDate: Instant?,
    ) {
        if (
            lectureStartDate.toEpochMilliseconds() < eventStartDate.toEpochMilliseconds() || (
                lectureFinishDate != null &&
                    eventFinishDate != null &&
                    lectureFinishDate.toEpochMilliseconds() > eventFinishDate.toEpochMilliseconds()
                )
        ) {
            throw BadRequestException("lecture timeframe not in event timeframe")
        }
    }
}