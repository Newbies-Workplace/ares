package pl.newbies.common

import com.expediagroup.graphql.server.execution.GraphQLRequestHandler
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.addDeserializer
import com.fasterxml.jackson.module.kotlin.addSerializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.datetime.Instant
import org.koin.dsl.module
import pl.newbies.common.graphql.*

val commonModule = module {
    single {
        jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .registerModule(
                SimpleModule()
                    .addDeserializer(Instant::class, InstantDeserializer())
                    .addSerializer(Instant::class, InstantSerializer())
            )
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }
    single { SchemaBuilder(get(), get(), get(), get()) }
    single { GraphQLHandler(get(), get()) }
    single { KtorDataLoaderRegistryFactory(get()) }
    single { KtorGraphQLRequestParser(get()) }
    single { KtorGraphQLContextFactory() }
    single { GraphQLRequestHandler(get<SchemaBuilder>().getGraphQLObject(), get<KtorDataLoaderRegistryFactory>()) }
    single { KtorGraphQLServer(get(), get(), get()) }
}

class InstantDeserializer : JsonDeserializer<Instant>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Instant =
        Instant.parse(p.valueAsString)
}
class InstantSerializer : JsonSerializer<Instant>() {
    override fun serialize(value: Instant, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeString(value.toString())
    }
}