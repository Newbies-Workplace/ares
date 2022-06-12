package pl.newbies.common.graphql

import com.expediagroup.graphql.server.execution.GraphQLRequestParser
import com.expediagroup.graphql.server.types.GraphQLServerRequest
import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.receive
import kotlinx.serialization.json.JsonElement
import java.io.IOException

class KtorGraphQLRequestParser(
    private val mapper: ObjectMapper
) : GraphQLRequestParser<ApplicationRequest> {

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun parseRequest(request: ApplicationRequest): GraphQLServerRequest =
        try {
            val rawRequest: JsonElement = request.call.receive()

            mapper.readValue(rawRequest.toString(), GraphQLServerRequest::class.java)
        } catch (e: Exception) {
            throw IOException("Unable to parse GraphQL payload.", e)
        }
}