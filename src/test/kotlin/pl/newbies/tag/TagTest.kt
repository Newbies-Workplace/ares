package pl.newbies.tag

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.newbies.tag.application.model.TagRequest
import pl.newbies.tag.application.model.TagResponse
import pl.newbies.util.IntegrationTest
import pl.newbies.util.TestData

class TagTest : IntegrationTest() {

    @Nested
    inner class Get {
        @Test
        fun `should return empty list when there are no tags`() = withAres {
            // when
            val response = httpClient.get("api/v1/tags")

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(emptyList<TagResponse>(), response.body<List<TagResponse>>())
        }
    }

    @Nested
    inner class GetMyTags {

        @Test
        fun `should return empty list when no tag is followed`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser3)

            // when
            val response = httpClient.get("api/v1/tags/@me") {
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(emptyList<TagResponse>(), response.body<List<TagResponse>>())
        }

        @Test
        fun `should return 401 when called without authentication`() = withAres {
            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.get("api/v1/tags/@me")
            }

            // then
            assertEquals(HttpStatusCode.Unauthorized, exception.response.status)
        }
    }

    @Nested
    inner class PutMyTags {

        @Test
        fun `should return 401 when called without authentication`() = withAres {
            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.put("api/v1/tags/@me") {
                    setBody(listOf(TagRequest("id")))
                    contentType(ContentType.Application.Json)
                }
            }

            // then
            assertEquals(HttpStatusCode.Unauthorized, exception.response.status)
        }
    }
}