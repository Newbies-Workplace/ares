package pl.newbies.util

import com.auth0.jwt.JWT
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import io.ktor.client.call.body
import io.ktor.client.plugins.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.testcontainers.containers.MariaDBContainer
import pl.newbies.auth.application.model.AuthResponse
import pl.newbies.auth.application.model.GithubUser
import pl.newbies.module

abstract class IntegrationTest {

    suspend fun ApplicationTestBuilder.loginAs(githubUser: GithubUser): AuthResponse {
        val response = httpClient.submitForm(
            url = "/oauth/callback/github",
            formParameters = Parameters.build {
                append("code", "valid")
                append("state", githubUser.id)
            },
            encodeInQuery = true,
        )

        assertEquals(HttpStatusCode.OK, response.status)

        return response.body()
    }

    val ApplicationTestBuilder.httpClient
        get() = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

    fun AuthResponse.getUserId(): String =
        JWT.decode(accessToken).claims["id"].toString().replace("\"", "")

    fun withAres(block: suspend ApplicationTestBuilder.() -> Unit) =
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

    companion object {
        private val container = MariaDBContainer("mariadb:10.4")
            .withDatabaseName("ares")
            .withReuse(true)

        init {
            container.start()
        }
    }
}