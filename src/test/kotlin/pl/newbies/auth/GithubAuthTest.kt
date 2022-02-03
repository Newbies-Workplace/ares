package pl.newbies.auth

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import pl.newbies.util.*

class GithubAuthTest : IntegrationTest() {

    @Nested
    inner class Login {
        @Test
        fun `should redirect when login called`() = withAres {
            // when
            val response = httpClient.get("/oauth/login/github")

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("ok", response.bodyAsText())
        }
    }

    @Nested
    inner class Callback {
        @Test
        fun `should create account when called with new user`() = withAres {
            // given
            clearTable("Users")
            val user = TestData.testUser1

            // when
            val response = loginAs(user)

            // then
            assertEquals(user.name, response.username)
            assertDoesNotThrow { getUser(response.getUserId()) }
        }

        @Test
        fun `should return existing account when called with existing github user`() = withAres {
            // given
            clearTable("Users")
            val user = TestData.testUser1
            val authResponse = loginAs(user)
            val createdUser = getUser(authResponse.getUserId())
            delay(1_000)

            // when
            val response = loginAs(user)
            val loggedUser = getUser(response.getUserId())

            // then
            assertEquals(createdUser.createDate, loggedUser.createDate)
            assertEquals(user.name, response.username)
        }

        @Test
        fun `should return 401 when called with invalid data`() = withAres {
            // then
            val exception = assertThrows<ClientRequestException> {
                client.submitForm(
                    url = "/oauth/callback/github",
                    formParameters = Parameters.build {
                        append("code", "valid")
                        append("state", "invalid")
                    },
                    encodeInQuery = true,
                )
            }

            assertEquals(HttpStatusCode.Unauthorized, exception.response.status)
        }
    }
}