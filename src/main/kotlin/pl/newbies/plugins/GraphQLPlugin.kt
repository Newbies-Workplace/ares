package pl.newbies.plugins

import com.expediagroup.graphql.generator.extensions.print
import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.application.pluginOrNull
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import pl.newbies.common.graphql.GraphQLHandler
import pl.newbies.common.graphql.SchemaBuilder

fun Application.graphQLModule(
    graphQLEndpoint: String = "graphql",
    subscriptionsEndpoint: String = "subscriptions",
    playgroundEnabled: Boolean = true,
) {
    val schemaBuilder: SchemaBuilder by inject()
    val graphQLHandler: GraphQLHandler by inject()

    pluginOrNull(Routing) ?: install(Routing)

    routing {
        authenticate("jwt", optional = true) {
            post(graphQLEndpoint) {
                graphQLHandler.handle(call)
            }
        }

        get("sdl") {
            call.respondText(schemaBuilder.graphQLSchema.print())
        }

        if (playgroundEnabled) {
            get(graphQLEndpoint) {
                call.respondText(
                    buildPlaygroundHtml(graphQLEndpoint, subscriptionsEndpoint),
                    ContentType.Text.Html,
                )
            }
        }
    }
}

private fun buildPlaygroundHtml(graphQLEndpoint: String, subscriptionsEndpoint: String) =
    Application::class.java.classLoader.getResource("graphql-playground.html")?.readText()
        ?.replace("\${graphQLEndpoint}", graphQLEndpoint)
        ?.replace("\${subscriptionsEndpoint}", subscriptionsEndpoint)
        ?: throw IllegalStateException("graphql-playground.html cannot be found in the classpath")