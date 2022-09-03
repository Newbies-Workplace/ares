package pl.newbies.common.graphql

import com.expediagroup.graphql.generator.SchemaGeneratorConfig
import com.expediagroup.graphql.generator.TopLevelObject
import com.expediagroup.graphql.generator.execution.SimpleKotlinDataFetcherFactoryProvider
import com.expediagroup.graphql.generator.toSchema
import graphql.GraphQL
import pl.newbies.event.application.EventSchema
import pl.newbies.lecture.application.LectureSchema
import pl.newbies.tag.application.TagSchema
import pl.newbies.user.application.UserSchema

class SchemaBuilder(
    tagSchema: TagSchema,
    eventSchema: EventSchema,
    userSchema: UserSchema,
    lectureSchema: LectureSchema,
) {
    private val config = SchemaGeneratorConfig(
        supportedPackages = listOf("pl.newbies"),
        hooks = CustomSchemaGeneratorHooks(),
        dataFetcherFactoryProvider = SimpleKotlinDataFetcherFactoryProvider()
    )

    private val queries = listOf(
        TopLevelObject(tagSchema.Query()),
        TopLevelObject(eventSchema.Query()),
        TopLevelObject(userSchema.Query()),
        TopLevelObject(lectureSchema.Query()),
    )
    private val mutations = listOf(
        TopLevelObject(tagSchema.Mutation()),
        TopLevelObject(eventSchema.Mutation()),
        TopLevelObject(userSchema.Mutation()),
        TopLevelObject(lectureSchema.Mutation()),
    )

    val graphQLSchema = toSchema(config, queries, mutations)

    fun getGraphQLObject(): GraphQL = GraphQL.newGraphQL(graphQLSchema).build()
}