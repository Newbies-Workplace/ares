package pl.newbies.user

import org.koin.dsl.module
import pl.newbies.user.application.UserConverter
import pl.newbies.user.application.UserSchema
import pl.newbies.user.domain.service.UserEditor
import pl.newbies.user.domain.service.UserService

val userModule = module {
    single { UserConverter(get()) }
    single { UserService(get()) }
    single { UserEditor() }
    single { UserSchema(get(), get()) }
}