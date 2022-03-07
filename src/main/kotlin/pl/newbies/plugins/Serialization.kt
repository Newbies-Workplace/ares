package pl.newbies.plugins

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.ContentNegotiation
import kotlinx.serialization.json.Json

val defaultJson = Json {
    encodeDefaults = true
    isLenient = true
    prettyPrint = true
    ignoreUnknownKeys = true
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(defaultJson)
    }
}
