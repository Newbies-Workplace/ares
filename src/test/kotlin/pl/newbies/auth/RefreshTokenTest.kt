package pl.newbies.auth

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.newbies.auth.application.model.AuthResponse
import pl.newbies.common.nanoId
import pl.newbies.util.IntegrationTest
import pl.newbies.util.TestData
import pl.newbies.util.httpClient
import pl.newbies.util.loginAs

class RefreshTokenTest : IntegrationTest() {

    @Nested
    inner class Refresh {
        @Test
        fun `should refresh token when refreshToken exists`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser3)
            val refreshToken = authResponse.refreshToken

            // when
            val response = httpClient.post("api/v1/refresh") {
                setBody(refreshToken)
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<AuthResponse>()
            assertNotEquals(refreshToken, responseBody)
            assertEquals(authResponse.getUserId(), responseBody.user.id)
        }

        @Test
        fun `should return 401 when refreshToken does not exists`() = withAres {
            // given
            val randomRefreshToken = nanoId()

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.post("api/v1/refresh") {
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
            val response = httpClient.delete("api/v1/logout") {
                setBody(refreshToken)
                bearerAuth(authResponse.accessToken)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val exception = assertThrows<ClientRequestException> {
                httpClient.post("api/v1/refresh") {
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
            val response = httpClient.delete("api/v1/logout") {
                setBody(refreshToken)
                bearerAuth(authResponse.accessToken)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val exception = assertThrows<ClientRequestException> {
                httpClient.delete("api/v1/logout") {
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
                httpClient.delete("api/v1/logout") {
                    setBody(refreshToken)
                }
            }

            // then
            assertEquals(HttpStatusCode.Unauthorized, exception.response.status)
        }
    }
}