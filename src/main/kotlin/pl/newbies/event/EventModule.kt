package pl.newbies.event

import org.koin.dsl.module
import pl.newbies.event.application.EventConverter
import pl.newbies.event.application.EventSchema
import pl.newbies.event.domain.service.EventService
import pl.newbies.plugins.prop
import pl.newbies.storage.application.FileUrlConverter

val eventModule = module {
    single { EventService() }
    single { EventConverter(get(), get()) }
    single { FileUrlConverter(prop("storage.url").getString()) }
    single { EventSchema(get(), get(), get(), get()) }
}