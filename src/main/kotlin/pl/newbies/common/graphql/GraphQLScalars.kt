package pl.newbies.common.graphql

import com.expediagroup.graphql.generator.hooks.SchemaGeneratorHooks
import graphql.language.StringValue
import graphql.schema.*
import kotlinx.datetime.Instant
import kotlin.reflect.KClass
import kotlin.reflect.KType

val instantType: GraphQLScalarType = GraphQLScalarType.newScalar()
    .name("Instant")
    .description("ISO date-time")
    .coercing(InstantCoercing)
    .build()

object InstantCoercing : Coercing<Instant, String> {
    override fun parseValue(input: Any): Instant =
        runCatching {
            Instant.parse(serialize(input))
        }.getOrElse { throw CoercingParseValueException("Expected valid Instant but was $input") }

    override fun parseLiteral(input: Any): Instant {
        val dateString = (input as? StringValue)?.value

        return runCatching {
            Instant.parse(dateString!!)
        }.getOrElse {
            throw CoercingParseLiteralException("Expected valid Instant but was $dateString")
        }
    }

    override fun serialize(dataFetcherResult: Any): String =
        runCatching {
            dataFetcherResult.toString()
        }.getOrElse {
            throw CoercingSerializeException("Data fetcher result $dataFetcherResult cannot be serialized to a String")
        }
}

class CustomSchemaGeneratorHooks : SchemaGeneratorHooks {

    override fun willGenerateGraphQLType(type: KType): GraphQLType? =
        when (type.classifier as? KClass<*>) {
            Instant::class -> instantType
            else -> null
        }
}