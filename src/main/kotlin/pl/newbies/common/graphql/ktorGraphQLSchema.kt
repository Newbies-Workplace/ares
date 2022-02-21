package pl.newbies.common.graphql

import com.expediagroup.graphql.generator.SchemaGeneratorConfig
import com.expediagroup.graphql.generator.TopLevelObject
import com.expediagroup.graphql.generator.toSchema
import graphql.GraphQL
import pl.newbies.lecture.application.LectureSchema
import pl.newbies.tag.application.TagSchema
import pl.newbies.user.application.UserSchema

class SchemaBuilder(
    tagSchema: TagSchema,
    lectureSchema: LectureSchema,
    userSchema: UserSchema,
) {
    private val config = SchemaGeneratorConfig(
        supportedPackages = listOf("pl.newbies"),
        hooks = CustomSchemaGeneratorHooks(),
    )

    private val queries = listOf(
        TopLevelObject(tagSchema.Query()),
        TopLevelObject(lectureSchema.Query()),
        TopLevelObject(userSchema.Query())
    )
    private val mutations = listOf(
        TopLevelObject(tagSchema.Mutation()),
        TopLevelObject(lectureSchema.Mutation()),
        TopLevelObject(userSchema.Mutation()),
    )

    val graphQLSchema = toSchema(config, queries, mutations)

    fun getGraphQLObject(): GraphQL = GraphQL.newGraphQL(graphQLSchema).build()
}

