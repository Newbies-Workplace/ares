package pl.newbies.lecture.domain.service

import io.ktor.server.plugins.BadRequestException
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.common.NotFoundException
import pl.newbies.common.nanoId
import pl.newbies.event.domain.model.Event
import pl.newbies.event.infrastructure.repository.EventDAO
import pl.newbies.event.infrastructure.repository.toEvent
import pl.newbies.lecture.application.model.LectureFilter
import pl.newbies.lecture.application.model.LectureInviteRequest
import pl.newbies.lecture.application.model.LectureRateRequest
import pl.newbies.lecture.application.model.LectureRequest
import pl.newbies.lecture.domain.SpeakerAlreadyInLectureException
import pl.newbies.lecture.domain.TooManyLectureInvitesException
import pl.newbies.lecture.domain.model.Lecture
import pl.newbies.lecture.domain.model.LectureInvite
import pl.newbies.lecture.domain.model.LectureRate
import pl.newbies.lecture.infrastructure.repository.*
import pl.newbies.plugins.AresPrincipal
import pl.newbies.user.infrastructure.repository.UserDAO
import pl.newbies.user.infrastructure.repository.Users

class LectureService {

    companion object {
        const val LECTURE_SPEAKERS_LIMIT = 2
    }

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

    fun createLectureInvite(
        lecture: Lecture,
        inviteRequest: LectureInviteRequest,
    ): LectureInvite = transaction {
        val databaseLecture = LectureDAO[lecture.id]
            .load(LectureDAO::speakers, LectureDAO::invites)

        val invitesCount = databaseLecture.invites.count()
        val speakersCount = databaseLecture.speakers.count()

        if (invitesCount + speakersCount >= LECTURE_SPEAKERS_LIMIT) {
            throw TooManyLectureInvitesException(lectureId = lecture.id)
        }

        LectureInviteDAO.new(nanoId()) {
            this.lecture = databaseLecture

            this.name = inviteRequest.name

            this.createDate = Clock.System.now()
        }.toInvite()
    }

    fun useLectureInvite(lecture: Lecture, invite: LectureInvite, user: AresPrincipal) = transaction {
        if (!LectureSpeakers.select { LectureSpeakers.user eq user.userId }.empty()) {
            throw SpeakerAlreadyInLectureException(
                userId = user.userId,
                lectureId = lecture.id,
            )
        }

        LectureSpeakers.insert {
            it[this.lecture] = lecture.id
            it[this.user] = user.userId
        }

        LectureInviteDAO[invite.id].delete()
    }

    fun deleteLecture(lecture: Lecture) = transaction {
        LectureDAO[lecture.id].delete()
    }

    fun rateLecture(lecture: Lecture, rateRequest: LectureRateRequest): LectureRate = transaction {
        val rate = LectureRateDAO.new(nanoId()) {
            this.lecture = LectureDAO[lecture.id]

            this.topicRate = rateRequest.topicRate
            this.presentationRate = rateRequest.presentationRate
            this.opinion = rateRequest.opinion

            this.createDate = Clock.System.now()
        }

        rate.toLectureRate()
    }

    fun deleteLectureInvite(invite: LectureInvite) = transaction {
        LectureInviteDAO[invite.id].delete()
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
        lectureFinishDate: Instant,
        eventStartDate: Instant,
        eventFinishDate: Instant,
    ) {
        if (
            lectureStartDate.toEpochMilliseconds() < eventStartDate.toEpochMilliseconds() ||
            lectureFinishDate.toEpochMilliseconds() > eventFinishDate.toEpochMilliseconds()
        ) {
            throw BadRequestException("lecture timeframe not in event timeframe")
        }
    }
}