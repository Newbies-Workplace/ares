package pl.newbies.common.graphql

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

class GraphQLHandler(
    private val objectMapper: ObjectMapper,
    private val ktorGraphQLServer: KtorGraphQLServer,
) {

    suspend fun handle(applicationCall: ApplicationCall) {
        val result = ktorGraphQLServer.execute(applicationCall.request)

        if (result != null) {
            val json = objectMapper.writeValueAsString(result)

            applicationCall.response.call.respond(json)
        } else {
            applicationCall.response.call.respond(HttpStatusCode.BadRequest, "Invalid request")
        }
    }
}