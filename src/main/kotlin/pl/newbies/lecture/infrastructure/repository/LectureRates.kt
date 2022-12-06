package pl.newbies.lecture.infrastructure.repository

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import pl.newbies.lecture.domain.model.LectureRate
import pl.newbies.plugins.StringNanoIdEntity
import pl.newbies.plugins.StringNanoIdEntityClass
import pl.newbies.plugins.StringNanoIdTable

object LectureRates : StringNanoIdTable() {
    val lecture = reference("lecture", Lectures, onDelete = ReferenceOption.CASCADE)

    val topicRate = integer("topicRate")
    val presentationRate = integer("presentationRate")
    val opinion = text("opinion", collate = "utf8mb4_unicode_ci").nullable()

    val createDate = timestamp("createDate")
}

class LectureRateDAO(id: EntityID<String>) : StringNanoIdEntity(id) {
    companion object : StringNanoIdEntityClass<LectureRateDAO>(LectureRates)

    var lecture by LectureDAO referencedOn LectureRates.lecture

    var topicRate by LectureRates.topicRate
    var presentationRate by LectureRates.presentationRate
    var opinion by LectureRates.opinion

    var createDate by LectureRates.createDate
}

fun LectureRateDAO.toLectureRate(): LectureRate =
    LectureRate(
        id = id.value,
        lectureId = lecture.id.value,
        topicRate = topicRate,
        presentationRate = presentationRate,
        opinion = opinion,
        createDate = createDate,
    )