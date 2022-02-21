package pl.newbies.common.graphql

import com.expediagroup.graphql.generator.execution.GraphQLContext
import com.expediagroup.graphql.server.execution.GraphQLContextFactory
import io.ktor.server.auth.principal
import io.ktor.server.request.ApplicationRequest
import pl.newbies.plugins.AresPrincipal

class KtorGraphQLContextFactory : GraphQLContextFactory<GraphQLContext, ApplicationRequest> {

    override suspend fun generateContextMap(request: ApplicationRequest): Map<*, Any> =
        mutableMapOf<Any, Any>()
            .also { map ->
                request.call.principal<AresPrincipal>()?.let { map["PRINCIPAL"] = it }
            }

    override suspend fun generateContext(request: ApplicationRequest): GraphQLContext? = null
}
