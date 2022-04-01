package pl.newbies.lecture

import org.koin.dsl.module
import pl.newbies.lecture.application.LectureConverter
import pl.newbies.lecture.application.LectureSchema
import pl.newbies.lecture.domain.service.LectureService
import pl.newbies.storage.application.FileUrlConverter

val lectureModule = module {
    single { LectureService() }
    single { LectureConverter(get()) }
    single { FileUrlConverter() }
    single { LectureSchema(get(), get(), get()) }
}