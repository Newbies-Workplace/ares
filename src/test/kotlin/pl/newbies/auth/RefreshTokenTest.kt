package pl.newbies.auth

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.*
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.newbies.auth.application.model.AuthResponse
import pl.newbies.util.IntegrationTest
import pl.newbies.util.TestData
import java.util.*

class RefreshTokenTest : IntegrationTest() {

    @Nested
    inner class Refresh {
        @Test
        fun `should refresh token when refreshToken exists`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser3)
            val refreshToken = authResponse.refreshToken

            // when
            val response = httpClient.post("api/refresh") {
                setBody(refreshToken)
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<AuthResponse>()
            assertNotEquals(refreshToken, responseBody)
        }

        @Test
        fun `should return 401 when refreshToken does not exists`() = withAres {
            // given
            val randomRefreshToken = UUID.randomUUID().toString()

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.post("api/refresh") {
                    setBody(randomRefreshToken)
                }
            }

            // then
            assertEquals(HttpStatusCode.Unauthorized, exception.response.status)
        }
    }

    @Nested
    inner class Logout {
        @Test
        fun `should return 200 when called with active refreshToken`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser2)
            val refreshToken = authResponse.refreshToken

            // when
            val response = httpClient.delete("api/logout") {
                setBody(refreshToken)
                bearerAuth(authResponse.accessToken)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val exception = assertThrows<ClientRequestException> {
                httpClient.post("api/refresh") {
                    setBody(refreshToken)
                }
            }

            // then
            assertEquals(HttpStatusCode.Unauthorized, exception.response.status)
        }

        @Test
        fun `should return 401 when called with disabled refreshToken`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser2)
            val refreshToken = authResponse.refreshToken

            // when
            val response = httpClient.delete("api/logout") {
                setBody(refreshToken)
                bearerAuth(authResponse.accessToken)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val exception = assertThrows<ClientRequestException> {
                httpClient.delete("api/logout") {
                    setBody(refreshToken)
                    bearerAuth(authResponse.accessToken)
                }
            }

            // then
            assertEquals(HttpStatusCode.Unauthorized, exception.response.status)
        }

        @Test
        fun `should return 401 when called without authentication`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser2)
            val refreshToken = authResponse.refreshToken

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.delete("api/logout") {
                    setBody(refreshToken)
                }
            }

            //then
            assertEquals(HttpStatusCode.Unauthorized, exception.response.status)
        }
    }
}