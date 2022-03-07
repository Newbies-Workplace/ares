package pl.newbies.common.graphql

import com.expediagroup.graphql.generator.execution.GraphQLContext
import com.expediagroup.graphql.server.execution.GraphQLContextFactory
import io.ktor.server.auth.principal
import io.ktor.server.request.ApplicationRequest
import pl.newbies.plugins.AresPrincipal

class KtorGraphQLContextFactory : GraphQLContextFactory<GraphQLContext, ApplicationRequest> {

    override suspend fun generateContextMap(request: ApplicationRequest): Map<*, Any> {
        val map = mutableMapOf<Any, Any>()

        request.call.principal<AresPrincipal>()
            ?.also { map["PRINCIPAL"] = it }

        return map
    }

    override suspend fun generateContext(request: ApplicationRequest): GraphQLContext? = null
}