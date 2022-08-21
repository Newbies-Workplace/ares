package pl.newbies.common.graphql

import com.expediagroup.graphql.generator.hooks.SchemaGeneratorHooks
import com.expediagroup.graphql.plugin.schema.hooks.SchemaGeneratorHooksProvider

/**
 * needed for schema generation task
 * https://opensource.expediagroup.com/graphql-kotlin/docs/plugins/gradle-plugin-tasks#graphqlgeneratesdl
 */
class CustomSchemaGeneratorHooksProvider : SchemaGeneratorHooksProvider {

    override fun hooks(): SchemaGeneratorHooks =
        CustomSchemaGeneratorHooks()
}