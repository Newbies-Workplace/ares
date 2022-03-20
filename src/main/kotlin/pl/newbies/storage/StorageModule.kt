package pl.newbies.storage

import org.koin.dsl.module
import pl.newbies.plugins.prop
import pl.newbies.storage.domain.StorageService

val storageModule = module {
    single { StorageService(prop("storage.path").getString()) }
}