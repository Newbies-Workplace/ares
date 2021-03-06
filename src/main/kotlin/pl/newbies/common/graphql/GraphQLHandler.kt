package pl.newbies.common.graphql

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.response.respondText

class GraphQLHandler(
    private val objectMapper: ObjectMapper,
    private val ktorGraphQLServer: KtorGraphQLServer,
) {

    suspend fun handle(applicationCall: ApplicationCall) {
        val result = ktorGraphQLServer.execute(applicationCall.request)

        if (result != null) {
            val json = objectMapper.writeValueAsString(result)

            applicationCall.respondText(json, ContentType.Text.Plain)
        } else {
            applicationCall.respond(HttpStatusCode.BadRequest, "Invalid request")
        }
    }
}