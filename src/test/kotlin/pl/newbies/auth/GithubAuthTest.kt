package pl.newbies.auth

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.newbies.util.IntegrationTest
import pl.newbies.util.TestData

class GithubAuthTest : IntegrationTest() {

    @Nested
    inner class Login  {
        @Test
        fun `redirects on login called`() = withAres {
            // when
            val response = client.get("/oauth/login/github")

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("ok", response.bodyAsText())
        }
    }

    @Nested
    inner class Callback {
        @Test
        fun `creates account when called with new user`() = withAres {
            // given
            val user = TestData.testUser1

            // when
            val response = loginAs(user)

            // then
            assertEquals(user.name, response.username)
        }

        @Test
        fun `returns existing account when called with existing github user`() = withAres {
            // given
            val user = TestData.testUser1

            // when
            val response = loginAs(user)

            // then
            assertEquals(user.name, response.username)
        }

        @Test
        fun `returns 401 on invalid data`() = withAres {
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