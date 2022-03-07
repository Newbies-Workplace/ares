package pl.newbies.common.graphql

import com.expediagroup.graphql.server.execution.GraphQLRequestHandler
import com.expediagroup.graphql.server.execution.GraphQLServer
import io.ktor.server.request.ApplicationRequest

class KtorGraphQLServer(
    requestParser: KtorGraphQLRequestParser,
    contextFactory: KtorGraphQLContextFactory,
    requestHandler: GraphQLRequestHandler,
) : GraphQLServer<ApplicationRequest>(requestParser, contextFactory, requestHandler)