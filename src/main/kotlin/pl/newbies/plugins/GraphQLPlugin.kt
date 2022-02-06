package pl.newbies.plugins

import com.apurebase.kgraphql.*
import com.apurebase.kgraphql.schema.Schema
import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
import com.apurebase.kgraphql.schema.dsl.SchemaConfigurationDSL
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.path
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import io.ktor.util.AttributeKey
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json.Default.decodeFromString
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// workaround for https://github.com/aPureBase/KGraphQL/pull/179
class GraphQLPlugin(val schema: Schema) {

    class Configuration : SchemaConfigurationDSL() {
        fun schema(block: SchemaBuilder.() -> Unit) {
            schemaBlock = block
        }

        var playground: Boolean = false

        var endpoint: String = "/graphql"

        fun context(block: ContextBuilder.(ApplicationCall) -> Unit) {
            contextSetup = block
        }

        fun wrap(block: Route.(next: Route.() -> Unit) -> Unit) {
            wrapWith = block
        }

        internal var contextSetup: (ContextBuilder.(ApplicationCall) -> Unit)? = null
        internal var wrapWith: (Route.(next: Route.() -> Unit) -> Unit)? = null
        internal var schemaBlock: (SchemaBuilder.() -> Unit)? = null
    }

    companion object Feature : ApplicationPlugin<Application, Configuration, GraphQLPlugin> {
        override val key = AttributeKey<GraphQLPlugin>("KGraphQL")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): GraphQLPlugin {
            val config = Configuration().apply(configure)
            val schema = KGraphQL.schema {
                configuration = config
                config.schemaBlock?.invoke(this)
            }

            val routing: Routing.() -> Unit = {
                val routing: Route.() -> Unit = {
                    route(config.endpoint) {
                        post {
                            val request = decodeFromString(GraphqlRequest.serializer(), call.receiveText())
                            val ctx = context {
                                config.contextSetup?.invoke(this, call)
                            }
                            val result = schema.execute(request.query, request.variables.toString(), ctx)
                            call.respondText(result, contentType = ContentType.Application.Json)
                        }
                        if (config.playground) {
                            get {
                                @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                                val playgroundHtml = KtorGraphQLConfiguration::class.java.classLoader.getResource("playground.html").readBytes()
                                call.respondBytes(playgroundHtml, contentType = ContentType.Text.Html)
                            }
                        }
                    }
                }

                config.wrapWith?.invoke(this, routing) ?: routing(this)
            }

            pipeline.pluginOrNull(Routing)?.apply(routing) ?: pipeline.install(Routing, routing)

            pipeline.intercept(ApplicationCallPipeline.Monitoring) {
                try {
                    coroutineScope {
                        proceed()
                    }
                } catch (e: Throwable) {
                    if (call.request.path() == config.endpoint) {
                        application.log.error("GraphQL error:")
                        context.respond(HttpStatusCode.OK, e.serialize())

                    } else throw e
                }
            }
            return GraphQLPlugin(schema)
        }

        private fun Throwable.serialize(): String = buildJsonObject {
            put("errors", buildJsonArray {
                addJsonObject {
                    put("message", message)
                    if (this@serialize is GraphQLError) {
                        put("locations", buildJsonArray {
                            locations?.forEach {
                                addJsonObject {
                                    put("line", it.line)
                                    put("column", it.column)
                                }
                            }
                        })
                    }
                    put("path", buildJsonArray { })
                }
            })
        }.toString()
    }
}