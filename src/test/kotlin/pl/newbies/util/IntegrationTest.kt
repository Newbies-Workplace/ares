package pl.newbies.util

import com.auth0.jwt.JWT
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import io.ktor.client.plugins.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.testcontainers.containers.MariaDBContainer
import pl.newbies.auth.application.model.AuthResponse
import pl.newbies.module

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
                    module(createClient {
                        this.install(ContentNegotiation) { json() } }
                    )
                }
            }

            externalServices {
                hosts("https://github.com", "https://api.github.com") { githubModule() }
            }

            block()
        }
    }

    companion object {
        private val container = MariaDBContainer("mariadb:10.4")
            .withUrlParam("characterEncoding", "utf-8")
            .withUrlParam("useUnicode", "true")
            .withDatabaseName("ares")
            .withUsername("root")
            .withPassword("")
            .withReuse(true)

        fun clearTable(tableName: String) {
            container.execInContainer("mysql", "-u", "root", "-p", "-D", "ares", "-e DELETE FROM $tableName;")
        }

        init {
            container.start()
        }
    }
}

val ApplicationTestBuilder.httpClient
    get() = createClient {
        install(ContentNegotiation) {
            json()
        }
    }