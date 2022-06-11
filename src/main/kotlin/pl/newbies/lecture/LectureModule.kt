package pl.newbies.lecture

import org.koin.dsl.module
import pl.newbies.lecture.application.LectureConverter
import pl.newbies.lecture.application.LectureSchema
import pl.newbies.lecture.domain.service.LectureService

val lectureModule = module {
    single { LectureService() }
    single { LectureConverter(get()) }
    single { LectureSchema(get(), get()) }
}