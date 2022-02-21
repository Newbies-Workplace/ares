package pl.newbies.common

import com.expediagroup.graphql.server.execution.GraphQLRequestHandler
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.koin.dsl.module
import pl.newbies.common.graphql.*

val commonModule = module {
    single { SchemaBuilder(get(), get(), get()) }
    single { GraphQLHandler(get()) }
    single { KtorDataLoaderRegistryFactory(get()) }
    single { KtorGraphQLRequestParser(jacksonObjectMapper()) }
    single { KtorGraphQLContextFactory() }
    single { GraphQLRequestHandler(get<SchemaBuilder>().getGraphQLObject(), get<KtorDataLoaderRegistryFactory>()) }
    single { KtorGraphQLServer(get(), get(), get()) }
}