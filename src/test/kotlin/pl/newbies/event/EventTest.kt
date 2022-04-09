package pl.newbies.event

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.*
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import org.junit.Ignore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import pl.newbies.common.nanoId
import pl.newbies.event.application.model.EventResponse
import pl.newbies.tag.application.model.TagResponse
import pl.newbies.util.*

class EventTest : IntegrationTest() {

    @Nested
    inner class GetAll {
        @Test
        fun `should return empty list when there are no events`() = withAres {
            // given
            clearTable("Events")

            // when
            val response = httpClient.get("api/v1/events")

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(emptyList<TagResponse>(), response.body<List<TagResponse>>())
        }

        @Test
        fun `should return events when there are some`() = withAres {
            // given
            clearTable("Events")
            val authResponse = loginAs(TestData.testUser1)
            val createdEvents = buildList {
                repeat(2) { add(createEvent(authResponse)) }
            }

            // when
            val response = httpClient.get("api/v1/events")

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<List<EventResponse>>().map { it.id }
            assertEquals(2, responseBody.size)
            assertTrue(createdEvents[0].id in responseBody)
            assertTrue(createdEvents[1].id in responseBody)
        }

        @Test
        fun `should return next page when there are enough items`() = withAres {
            // given
            clearTable("Events")
            val authResponse = loginAs(TestData.testUser1)
            val createdEvents = buildList {
                repeat(2) { add(createEvent(authResponse)) }
            }

            // when
            val response = httpClient.get("api/v1/events") {
                parameter("page", 2L)
                parameter("size", 1L)
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<List<EventResponse>>().map { it.id }
            assertEquals(1, responseBody.size)
            assertTrue(createdEvents[1].id in responseBody)
        }

        @Test
        fun `should return empty list when empty page requested`() = withAres {
            // given
            clearTable("Events")
            val authResponse = loginAs(TestData.testUser1)
            repeat(2) {
                createEvent(authResponse)
            }

            // when
            val response = httpClient.get("api/v1/events") {
                parameter("page", 3L)
                parameter("size", 1L)
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<List<EventResponse>>()
            assertEquals(emptyList<List<EventResponse>>(), responseBody)
        }
    }

    @Nested
    inner class GetById {
        @Test
        fun `should return 404 when there is no event with given id`() = withAres {
            // given
            clearTable("Events")
            val randomId = nanoId()

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.get("api/v1/events/$randomId")
            }

            // then
            assertEquals(HttpStatusCode.NotFound, exception.response.status)
        }

        @Test
        fun `should return existing event when called with valid id`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse)

            // when
            val response = httpClient.get("api/v1/events/${event.id}")

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<EventResponse>()
            assertEquals(event.id, responseBody.id)
        }
    }

    @Nested
    inner class Post {
        @Test
        fun `should return 401 when called without authentication`() = withAres {
            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.post("api/v1/events") {
                    setBody(TestData.createEventRequest())
                    contentType(ContentType.Application.Json)
                }
            }

            // then
            assertEquals(HttpStatusCode.Unauthorized, exception.response.status)
        }

        @Ignore
        @Test
        fun `should return 400 when called with invalid data`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val body = """
                {}
            """.trimIndent()

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.post("api/v1/events") {
                    setBody(Json.parseToJsonElement(body))
                    contentType(ContentType.Application.Json)
                    bearerAuth(authResponse.accessToken)
                }
            }

            // then
            assertEquals(HttpStatusCode.BadRequest, exception.response.status)
        }
    }

    @Nested
    inner class Put {
        @Test
        fun `should replace old data when called`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse = authResponse)
            val body = TestData.createEventRequest(title = "NewTitle")

            // when
            val response = httpClient.put("api/v1/events/${event.id}") {
                setBody(body)
                contentType(ContentType.Application.Json)
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<EventResponse>()
            assertEquals(body.title, responseBody.title)
        }

        @Ignore
        @Test
        fun `should return 400 when called with invalid data`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse = authResponse)
            val body = """
                {}
            """.trimIndent()

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.put("api/v1/events/${event.id}") {
                    setBody(Json.parseToJsonElement(body))
                    contentType(ContentType.Application.Json)
                    bearerAuth(authResponse.accessToken)
                }
            }

            // then
            assertEquals(HttpStatusCode.BadRequest, exception.response.status)
        }

        @Test
        fun `should return 401 when called without authentication`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse = authResponse)

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.put("api/v1/events/${event.id}") {
                    setBody(TestData.createEventRequest(title = "NewTitle"))
                    contentType(ContentType.Application.Json)
                }
            }

            // then
            assertEquals(HttpStatusCode.Unauthorized, exception.response.status)
        }

        @Test
        fun `should return 404 when called with not existing id`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val randomId = nanoId()

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.put("api/v1/events/$randomId") {
                    setBody(TestData.createEventRequest(title = "NewTitle"))
                    contentType(ContentType.Application.Json)
                    bearerAuth(authResponse.accessToken)
                }
            }

            // then
            assertEquals(HttpStatusCode.NotFound, exception.response.status)
        }

        @Test
        fun `should return 403 when called by another user`() = withAres {
            // given
            val firstAuthResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse = firstAuthResponse)
            val secondAuthResponse = loginAs(TestData.testUser2)

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.delete("api/v1/events/${event.id}") {
                    setBody(TestData.createEventRequest(title = "NewTitle"))
                    contentType(ContentType.Application.Json)
                    bearerAuth(secondAuthResponse.accessToken)
                }
            }

            // then
            assertEquals(HttpStatusCode.Forbidden, exception.response.status)
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `should return 401 when called without authentication`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse = authResponse)

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.delete("api/v1/events/${event.id}")
            }

            // then
            assertEquals(HttpStatusCode.Unauthorized, exception.response.status)
        }

        @Test
        fun `should return 404 when called with not existing id`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val randomId = nanoId()

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.delete("api/v1/events/$randomId") {
                    bearerAuth(authResponse.accessToken)
                }
            }

            // then
            assertEquals(HttpStatusCode.NotFound, exception.response.status)
        }

        @Test
        fun `should return 403 when called by another user`() = withAres {
            // given
            val firstAuthResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse = firstAuthResponse)
            val secondAuthResponse = loginAs(TestData.testUser2)

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.delete("api/v1/events/${event.id}") {
                    bearerAuth(secondAuthResponse.accessToken)
                }
            }

            // then
            assertEquals(HttpStatusCode.Forbidden, exception.response.status)
        }

        @Test
        fun `should delete when called by author`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse)

            // when
            val response = httpClient.delete("api/v1/events/${event.id}") {
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val exception = assertThrows<ClientRequestException> {
                httpClient.get("api/v1/events/${event.id}")
            }
            assertEquals(HttpStatusCode.NotFound, exception.response.status)
        }

        @Test
        fun `should delete storage directory when delete called`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse)
            addEventImage(
                authResponse = authResponse,
                eventId = event.id,
                imagePath = "images/newbies-logo.webp",
                contentType = "image/webp",
                fileName = "filename=newbies-logo.webp",
            )
            assertFileExists("events/${event.id}")

            // when
            val response = httpClient.delete("api/v1/events/${event.id}") {
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            assertFileNotExists("events/${event.id}")
        }
    }

    @Nested
    inner class PutThemeImage {
        @Test
        fun `should return 401 when called without authentication`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse = authResponse)

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.put("api/v1/events/${event.id}/theme/image")
            }

            // then
            assertEquals(HttpStatusCode.Unauthorized, exception.response.status)
        }

        @Test
        fun `should return 404 when called with not existing id`() = withAres {
            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.put("api/v1/events/randomid/theme/image")
            }

            // then
            assertEquals(HttpStatusCode.Unauthorized, exception.response.status)
        }

        @Test
        fun `should 400 return when called with unsupported file`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse = authResponse)

            // when
            val exception = assertThrows<ClientRequestException> {
                addEventImage(
                    authResponse = authResponse,
                    eventId = event.id,
                    imagePath = "images/newbies-logo.gif",
                    contentType = "image/gif",
                    fileName = "filename=newbies-logo.gif",
                )
            }

            // then
            assertEquals(HttpStatusCode.BadRequest, exception.response.status)
        }

        @Test
        fun `should 400 return when called without file`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse = authResponse)

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.put("/api/v1/events/${event.id}/theme/image") {
                    bearerAuth(authResponse.accessToken)
                    setBody(MultiPartFormDataContent(parts = listOf()))
                }
            }

            // then
            assertEquals(HttpStatusCode.BadRequest, exception.response.status)
        }

        @Test
        fun `should return 403 when called by another user`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val secondAuthResponse = loginAs(TestData.testUser2)
            val event = createEvent(authResponse = authResponse)

            // when
            val exception = assertThrows<ClientRequestException> {
                addEventImage(
                    authResponse = secondAuthResponse,
                    eventId = event.id,
                    imagePath = "images/newbies-logo.png",
                    contentType = "image/png",
                    fileName = "filename=newbies-logo.png",
                )
            }

            // then
            assertEquals(HttpStatusCode.Forbidden, exception.response.status)
        }

        @ParameterizedTest
        @CsvSource(value = [
            "images/newbies-logo.jpg,image/jpg,newbies-logo.jpg",
            "images/newbies-logo.png,image/png,newbies-logo.png",
            "images/newbies-logo.webp,image/webp,newbies-logo.webp",
        ])
        fun `should create image on valid request`(
            imagePath: String,
            contentType: String,
            fileName: String,
        ) = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse = authResponse)

            // when
            val responseBody = addEventImage(
                authResponse = authResponse,
                eventId = event.id,
                imagePath = imagePath,
                contentType = contentType,
                fileName = fileName,
            )

            // then
            assertEquals(responseBody.url, "http://localhost:80/api/v1/files/events/${event.id}/image.webp")
            assertFileExists("events/${event.id}/image.webp")
        }
    }

    @Nested
    inner class DeleteThemeImage {
        @Test
        fun `should return 401 when called without authentication`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse = authResponse)

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.delete("api/v1/events/${event.id}/theme/image")
            }

            // then
            assertEquals(HttpStatusCode.Unauthorized, exception.response.status)
        }

        @Test
        fun `should return 404 when called with not existing id`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.delete("api/v1/events/someRandomId/theme/image") {
                    bearerAuth(authResponse.accessToken)
                }
            }

            // then
            assertEquals(HttpStatusCode.NotFound, exception.response.status)
        }

        @Test
        fun `should return 403 when called by another user`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val secondAuthResponse = loginAs(TestData.testUser2)
            val event = createEvent(authResponse)
            addEventImage(
                authResponse = authResponse,
                eventId = event.id,
                imagePath = "images/newbies-logo.png",
                contentType = "image/png",
                fileName = "filename=newbies-logo.png",
            )

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.delete("api/v1/events/${event.id}/theme/image") {
                    bearerAuth(secondAuthResponse.accessToken)
                }
            }

            // then
            assertEquals(HttpStatusCode.Forbidden, exception.response.status)
        }

        @Test
        fun `should return 200 when event does not have image`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse)

            // when
            val response = httpClient.delete("api/v1/events/${event.id}/theme/image") {
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
        }

        @Test
        fun `should remove image when called on event with image`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse)
            addEventImage(
                authResponse = authResponse,
                eventId = event.id,
                imagePath = "images/newbies-logo.png",
                contentType = "image/png",
                fileName = "filename=newbies-logo.png",
            )
            assertFileExists("events/${event.id}/image.webp")

            // when
            val response = httpClient.delete("api/v1/events/${event.id}/theme/image") {
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertFileNotExists("events/${event.id}/image.webp")
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }
}