package pl.newbies.lecture.application

import pl.newbies.event.application.model.TimeFrameResponse
import pl.newbies.lecture.application.model.LectureResponse
import pl.newbies.lecture.domain.model.Lecture
import pl.newbies.user.application.UserConverter

class LectureConverter(
    private val userConverter: UserConverter,
) {

    fun convert(lecture: Lecture): LectureResponse =
        LectureResponse(
            id = lecture.id,
            title = lecture.title,
            description = lecture.description,
            timeFrame = lecture.timeFrame.let { timeFrame ->
                TimeFrameResponse(
                    startDate = timeFrame.startDate,
                    finishDate = timeFrame.finishDate
                )
            },
            createDate = lecture.createDate,
            updateDate = lecture.updateDate,
            author = userConverter.convert(lecture.author),
            speakers = lecture.speakers.map { userConverter.convert(it) },
        )
}