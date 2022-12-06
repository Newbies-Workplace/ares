package pl.newbies.lecture.application

import pl.newbies.lecture.application.model.LectureRateResponse
import pl.newbies.lecture.domain.model.LectureRate

class LectureRateConverter {

    fun convert(lectureRate: LectureRate): LectureRateResponse =
        LectureRateResponse(
            id = lectureRate.id,
            lectureId = lectureRate.lectureId,
            topicRate = lectureRate.topicRate,
            presentationRate = lectureRate.presentationRate,
            opinion = lectureRate.opinion,
            createDate = lectureRate.createDate,
        )
}