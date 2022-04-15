package pl.newbies.util

import com.auth0.jwt.JWT
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.TestInstance
import org.koin.core.context.stopKoin
import org.testcontainers.containers.MariaDBContainer
import pl.newbies.auth.application.model.AuthResponse
import pl.newbies.module

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
abstract class IntegrationTest {

    fun AuthResponse.getUserId(): String =
        JWT.decode(accessToken).claims["id"].toString().replace("\"", "")

    fun withAres(block: suspend ApplicationTestBuilder.() -> Unit) {
        testApplication {
            val configFactory = ConfigFactory.load()
                .withValue("database.jdbcUrl", ConfigValueFactory.fromAnyRef(container.jdbcUrl))

            environment {
                config = HoconApplicationConfig(configFactory)
                module {
                    module(
                        this@testApplication.createClient {
                            this.install(ContentNegotiation) { json() }
                        }
                    )
                }
            }

            externalServices {
                hosts("https://github.com", "https://api.github.com") { githubModule() }
            }

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

            container.execInContainer("mysql", "-u", "root", "-p", "-D", "ares", "-e DELETE FROM $tableName;")
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