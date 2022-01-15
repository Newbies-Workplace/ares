package pl.newbies.plugins

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.*
import io.ktor.server.application.*
import kotlinx.serialization.json.Json

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            encodeDefaults = true
            isLenient = true
            prettyPrint = true
            ignoreUnknownKeys = true
        })
    }
}
