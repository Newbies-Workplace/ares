package pl.newbies.auth

import org.koin.dsl.module
import pl.newbies.auth.domain.service.AuthService
import pl.newbies.plugins.prop

val authModule = module {
    single { AuthService(prop("jwt.secret").getString(), prop("jwt.issuer").getString()) }
}