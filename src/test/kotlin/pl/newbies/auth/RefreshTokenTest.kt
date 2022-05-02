package pl.newbies.auth

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
        fun `should return new refresh token when previous refreshToken exists`() = withAres {
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
            assertNotEquals(refreshToken, responseBody.refreshToken)
            assertEquals(authResponse.getUserId(), responseBody.user.id)
        }

        @Test
        fun `should disable previous token when new token is created`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser3)
            val firstRefreshToken = authResponse.refreshToken

            // when
            val secondResponse = httpClient.post("api/v1/refresh") {
                setBody(firstRefreshToken)
            }
            assertEquals(HttpStatusCode.OK, secondResponse.status)
            val secondRefreshToken = secondResponse.body<AuthResponse>().refreshToken
            assertNotEquals(firstRefreshToken, secondRefreshToken)

            // then
            val firstTokenRequest = httpClient.post("api/v1/refresh") {
                setBody(firstRefreshToken)
            }
            assertEquals(HttpStatusCode.Unauthorized, firstTokenRequest.status)
        }

        @Test
        fun `should disable token family when previous token is used twice`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser3)
            val firstRefreshToken = authResponse.refreshToken
            val secondResponse = httpClient.post("api/v1/refresh") {
                setBody(firstRefreshToken)
            }
            assertEquals(HttpStatusCode.OK, secondResponse.status)
            val secondRefreshToken = secondResponse.body<AuthResponse>().refreshToken
            assertNotEquals(firstRefreshToken, secondRefreshToken)

            // when
            val thirdResponse = httpClient.post("api/v1/refresh") {
                setBody(firstRefreshToken)
            }
            assertEquals(HttpStatusCode.Unauthorized, thirdResponse.status)

            // then
            val secondTokenResponse = httpClient.post("api/v1/refresh") {
                setBody(secondRefreshToken)
            }
            assertEquals(HttpStatusCode.Unauthorized, secondTokenResponse.status)
        }

        @Test
        fun `should return 401 when refreshToken does not exists`() = withAres {
            // given
            val randomRefreshToken = nanoId()

            // when
            val response = httpClient.post("api/v1/refresh") {
                setBody(randomRefreshToken)
            }

            // then
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        @Test
        fun `should return 401 when refresh token is expired`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser3)
            val refreshToken = authResponse.refreshToken
            // expire token in database
            executeSQL("UPDATE RefreshTokens SET dateExpired = '2022-01-01 12:00:00.000' WHERE id = '$refreshToken'")

            // when
            val response = httpClient.post("api/v1/refresh") {
                setBody(refreshToken)
            }

            // then
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            val body = response.bodyAsText()
            assertEquals("Token has expired", body)
        }
    }

    @Nested
    inner class Logout {
        @Test
        fun `should return 200 when called with active refreshToken`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser2)
            val refreshToken = authResponse.refreshToken
            val status = httpClient.delete("api/v1/logout") {
                setBody(refreshToken)
                bearerAuth(authResponse.accessToken)
            }.status
            assertEquals(HttpStatusCode.OK, status)

            // when
            val response = httpClient.post("api/v1/refresh") {
                setBody(refreshToken)
            }

            // then
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        @Test
        fun `should return 401 when called with disabled refreshToken`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser2)
            val refreshToken = authResponse.refreshToken
            val status = httpClient.delete("api/v1/logout") {
                setBody(refreshToken)
                bearerAuth(authResponse.accessToken)
            }.status
            assertEquals(HttpStatusCode.OK, status)

            // when
            val response = httpClient.delete("api/v1/logout") {
                setBody(refreshToken)
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        @Test
        fun `should return 401 when called without authentication`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser2)
            val refreshToken = authResponse.refreshToken

            // when
            val response = httpClient.delete("api/v1/logout") {
                setBody(refreshToken)
            }

            // then
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }
}