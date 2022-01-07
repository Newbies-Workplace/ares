package pl.newbies.tag

import org.koin.dsl.module
import pl.newbies.tag.application.TagConverter
import pl.newbies.tag.domain.service.TagService

val tagModule = module {
    single { TagConverter() }
    single { TagService() }
}