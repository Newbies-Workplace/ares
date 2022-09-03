package pl.newbies.util

import com.auth0.jwt.JWT
import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestInstance
import org.koin.core.context.stopKoin
import org.testcontainers.containers.MariaDBContainer
import pl.newbies.auth.application.model.AuthResponse
import pl.newbies.module
import java.net.URL

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
abstract class IntegrationTest {

    fun AuthResponse.getUserId(): String =
        JWT.decode(accessToken).claims["id"].toString().replace("\"", "")

    fun withAres(block: suspend ApplicationTestBuilder.() -> Unit) {
        testApplication {
            val configFactory = ConfigFactory.load()
                .withValue("database.jdbcUrl", ConfigValueFactory.fromAnyRef(container.jdbcUrl))

            application {
                module(
                    oauthClient = this@testApplication.createClient {
                        this.install(ContentNegotiation) { json() }
                    }
                )
            }

            environment {
                config = HoconApplicationConfig(configFactory)
            }

            externalServices {
                hosts("https://github.com", "https://api.github.com") { githubModule() }
            }


            // executed to initialize application (early execution of container.execInContainer fix)
            client.get("")

            block()
        }
    }

    @AfterEach
    fun cleanup() {
        stopKoin()
    }

    companion object {
        private val container = MariaDBContainer("mariadb:10.7")
            .withUrlParam("characterEncoding", "utf-8")
            .withUrlParam("useUnicode", "true")
            .withDatabaseName("ares")
            .withUsername("root")
            .withPassword("")
            .withReuse(true)

        fun clearTable(tableName: String) {
            println("[Test] Removing table $tableName")

            executeSQL("DELETE FROM $tableName;")
        }

        fun executeSQL(query: String) {
            val result = container.execInContainer("mysql", "-u", "root", "-D", "ares", "-e $query")

            assertEquals("", result.stderr, "executeSQL Failed with an error: ")
        }

        init {
            container.start()
        }

        @AfterAll
        @JvmStatic
        fun cleanupStorage() {
            removeDirectory("")
        }
    }
}

val ApplicationTestBuilder.httpClient
    get() = createClient {
        install(ContentNegotiation) {
            json()
        }
    }

val ApplicationTestBuilder.graphQLClient
    get() = GraphQLKtorClient(URL("http://localhost:80/graphql"), httpClient)