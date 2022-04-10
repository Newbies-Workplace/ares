package pl.newbies.common

import io.ktor.server.application.ApplicationCall
import kotlinx.serialization.decodeFromString
import pl.newbies.plugins.defaultJson

inline fun <reified R : Any> ApplicationCall.query(name: String): R? =
    request.queryParameters[name]?.let {
        defaultJson.decodeFromString(it)
    }
