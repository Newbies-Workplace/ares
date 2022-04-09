package pl.newbies.tag

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.newbies.common.nanoId
import pl.newbies.tag.application.model.TagCreateRequest
import pl.newbies.tag.application.model.TagRequest
import pl.newbies.tag.application.model.TagResponse
import pl.newbies.util.*

class TagTest : IntegrationTest() {

    @Nested
    inner class Get {
        @Test
        fun `should return empty list when there are no tags`() = withAres {
            // given
            clearTable("Tags")

            // when
            val response = httpClient.get("api/v1/tags")

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(emptyList<TagResponse>(), response.body<List<TagResponse>>())
        }

        @Test
        fun `should return all tags when there are some`() = withAres {
            // given
            clearTable("Tags")
            val authResponse = loginAs(TestData.testUser3)
            val createdTags = buildList {
                repeat(2) { add(createTag(authResponse = authResponse)) }
                add(TagResponse("notExistingId", "notExistingName"))
            }

            // when
            val response = httpClient.get("api/v1/tags")

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<List<TagResponse>>()
            assertEquals(2, responseBody.size)
            assertTrue(createdTags[0] in responseBody)
            assertTrue(createdTags[1] in responseBody)
        }
    }

    @Nested
    inner class Post {
        @Test
        fun `should create tag when called with valid name`() = withAres {
            // given
            val name = nanoId()
            val authResponse = loginAs(TestData.testUser3)
            val body = TagCreateRequest(name)

            // when
            val response = httpClient.post("api/v1/tags") {
                setBody(body)
                contentType(ContentType.Application.Json)
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<TagResponse>()

            assertEquals(name, responseBody.name)
        }

        @Test
        fun `should return 400 when called with blank name`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser3)
            val body = Json.parseToJsonElement("""{"name":" "}""")

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.post("api/v1/tags") {
                    setBody(body)
                    contentType(ContentType.Application.Json)
                    bearerAuth(authResponse.accessToken)
                }
            }

            // then
            assertEquals(HttpStatusCode.BadRequest, exception.response.status)
        }

        @Test
        fun `should return 409 when called with existing tag name`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser3)
            val name = nanoId()
            val body = TagCreateRequest(name)
            val firstResponse = httpClient.post("api/v1/tags") {
                setBody(body)
                contentType(ContentType.Application.Json)
                bearerAuth(authResponse.accessToken)
            }
            assertEquals(HttpStatusCode.OK, firstResponse.status)
            val firstResponseBody = firstResponse.body<TagResponse>()
            assertEquals(name, firstResponseBody.name)

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.post("api/v1/tags") {
                    setBody(body)
                    contentType(ContentType.Application.Json)
                    bearerAuth(authResponse.accessToken)
                }
            }

            // then
            assertEquals(HttpStatusCode.Conflict, exception.response.status)
        }

        @Test
        fun `should return 401 when called without authentication`() = withAres {
            // given
            val body = TagCreateRequest("name")

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.post("api/v1/tags") {
                    setBody(body)
                    contentType(ContentType.Application.Json)
                }
            }

            // then
            assertEquals(HttpStatusCode.Unauthorized, exception.response.status)
        }
    }

    @Nested
    inner class GetMyTags {
        @Test
        fun `should return empty list when no tag is followed`() = withAres {
            // given
            clearTable("Tags")
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
        fun `should return followed tags when there are some`() = withAres {
            // given
            clearTable("Tags")
            val authResponse = loginAs(TestData.testUser3)
            val existingTag = createTag(authResponse = authResponse)
            followTags(authResponse, existingTag.id)

            // when
            val response = httpClient.get("api/v1/tags/@me") {
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(existingTag, response.body<List<TagResponse>>().first())
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
        fun `should ignore not existing tags when called`() = withAres {
            // given
            clearTable("Tags")
            val authResponse = loginAs(TestData.testUser3)
            val createdTags = buildList {
                repeat(2) { add(createTag(authResponse = authResponse)) }
                add(TagResponse("notExistingId", "notExistingName"))
            }

            // when
            val response = httpClient.put("api/v1/tags/@me") {
                setBody(createdTags.map { TagRequest(it.id) })
                contentType(ContentType.Application.Json)
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<List<TagResponse>>()

            assertEquals(2, responseBody.size)
            assertTrue(createdTags[0] in responseBody)
            assertTrue(createdTags[1] in responseBody)
            assertFalse(createdTags[2] in responseBody)
        }

        @Test
        fun `should set followed tags when called`() = withAres {
            // given
            clearTable("Tags")
            val authResponse = loginAs(TestData.testUser3)
            val createdTags = buildList {
                repeat(10) { add(createTag(authResponse = authResponse)) }
            }

            // when
            val response = httpClient.put("api/v1/tags/@me") {
                setBody(createdTags.map { TagRequest(it.id) })
                contentType(ContentType.Application.Json)
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<List<TagResponse>>()

            assertEquals(createdTags.sortedBy { it.name }, responseBody.sortedBy { it.name })
        }

        @Test
        fun `should add new followed tags when called`() = withAres {
            // given
            clearTable("Tags")
            val authResponse = loginAs(TestData.testUser3)
            val createdTags = buildList {
                repeat(10) { add(createTag(authResponse = authResponse)) }
            }.sortedBy { it.name }
            followTags(
                authResponse = authResponse,
                ids = arrayOf(
                    createdTags[0].id,
                    createdTags[1].id,
                    createdTags[2].id,
                )
            )
            followTags(
                authResponse = authResponse,
                ids = arrayOf(
                    createdTags[3].id,
                    createdTags[4].id,
                    createdTags[5].id,
                )
            )

            // when
            val response = httpClient.get("api/v1/tags/@me") {
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<List<TagResponse>>().sortedBy { it.name }

            assertTrue(createdTags[0] in responseBody)
            assertTrue(createdTags[1] in responseBody)
            assertTrue(createdTags[2] in responseBody)
            assertTrue(createdTags[3] in responseBody)
            assertTrue(createdTags[4] in responseBody)
            assertTrue(createdTags[5] in responseBody)
            assertTrue(createdTags[6] !in responseBody)
        }

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