package pl.newbies.event

import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.*
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import pl.newbies.auth.application.model.AuthResponse
import pl.newbies.common.nanoId
import pl.newbies.event.application.model.EventFilter
import pl.newbies.event.application.model.EventResponse
import pl.newbies.event.application.model.EventThemeRequest
import pl.newbies.event.application.model.EventVisibilityRequest
import pl.newbies.event.domain.model.Event
import pl.newbies.plugins.defaultJson
import pl.newbies.storage.application.model.FileUrlResponse
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

        @ParameterizedTest
        @EnumSource(value = Event.Visibility::class, mode = EnumSource.Mode.EXCLUDE, names = ["PUBLIC"])
        fun `should not append to list when event is not public`(
            visibility: Event.Visibility
        ) = withAres {
            // given
            clearTable("Events")
            val authResponse = loginAs(TestData.testUser1)
            createEvent(authResponse, visibility = visibility)

            // when
            val response = httpClient.get("api/v1/events")

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<List<EventResponse>>()
            assertEquals(emptyList<List<EventResponse>>(), responseBody)
        }

        @Nested
        inner class Filtered {

            @ParameterizedTest
            @MethodSource("pl.newbies.event.EventTest#filterTestCases")
            fun `should return expected visibilities when requested`(case: FilterTestCase) = withAres {
                // given
                clearTable("Events")
                val authResponse = loginAs(TestData.testUser1)
                prepareFilterTestEvents(authResponse)

                // when
                val response = httpClient.get("api/v1/events") {
                    parameter("filter", defaultJson.encodeToJsonElement(case.filter))
                    when (case.requester) {
                        EventRequester.AUTHOR -> bearerAuth(authResponse.accessToken)
                        EventRequester.ANOTHER_USER -> bearerAuth(loginAs(TestData.testUser2).accessToken)
                        EventRequester.UNAUTHORIZED -> Unit
                    }
                }

                // then
                assertEquals(HttpStatusCode.OK, response.status)
                val body = response.body<List<EventResponse>>()
                assertEquals(case.expectedSize, body.size)
                assertTrue(body.all { it.visibility in case.expectedVisibilities })
            }
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
            val response = httpClient.get("api/v1/events/$randomId")

            // then
            assertEquals(HttpStatusCode.NotFound, response.status)
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

        @ParameterizedTest
        @CsvSource(
            value = [
                "200,PRIVATE,AUTHOR",
                "404,PRIVATE,ANOTHER_USER",
                "404,PRIVATE,UNAUTHORIZED",
                "200,INVISIBLE,AUTHOR",
                "200,INVISIBLE,ANOTHER_USER",
                "200,INVISIBLE,UNAUTHORIZED",
                "200,PUBLIC,AUTHOR",
                "200,PUBLIC,ANOTHER_USER",
                "200,PUBLIC,UNAUTHORIZED",
            ]
        )
        fun `should return correct status when event has specific visibility and requested by specific user`(
            status: Int,
            visibility: Event.Visibility,
            requester: EventRequester,
        ) = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse, visibility = visibility)

            // when
            val response = httpClient.get("api/v1/events/${event.id}") {
                expectSuccess = false
                when (requester) {
                    EventRequester.AUTHOR -> bearerAuth(authResponse.accessToken)
                    EventRequester.ANOTHER_USER -> bearerAuth(loginAs(TestData.testUser2).accessToken)
                    EventRequester.UNAUTHORIZED -> Unit
                }
            }

            // then
            assertEquals(status, response.status.value)
        }
    }

    @Nested
    inner class Post {
        @Test
        fun `should return 401 when called without authentication`() = withAres {
            // when
            val response = httpClient.post("api/v1/events") {
                setBody(TestData.createEventRequest())
                contentType(ContentType.Application.Json)
            }

            // then
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        @Test
        fun `should return 400 when called with invalid data`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val body = """
                {}
            """.trimIndent()

            // when
            val response = httpClient.post("api/v1/events") {
                setBody(Json.parseToJsonElement(body))
                contentType(ContentType.Application.Json)
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

        @Test
        fun `should create event when called with valid request`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)

            // when
            val event = createEvent(authResponse)

            // then
            assertNotNull(event.id)
        }

        @Nested
        inner class VanityUrl {
            @Test
            fun `should generate same vanityUrl when called with same event title`() = withAres {
                // given
                clearTable("Events")
                val title = "Once upon a time in lorem ipsum world"
                val authResponse = loginAs(TestData.testUser1)
                val firstEvent = createEvent(authResponse, TestData.createEventRequest(title = title))

                // when
                val secondEvent = createEvent(authResponse, TestData.createEventRequest(title = title))

                // then
                val firstVanityUrl = firstEvent.vanityUrl.substringBeforeLast('-')
                val secondVanityUrl = secondEvent.vanityUrl.substringBeforeLast('-')
                assertEquals(firstVanityUrl, secondVanityUrl)
            }

            @Test
            fun `should return just id when called with strange title`() = withAres {
                // given
                clearTable("Events")
                val title = "<><><><><><><><><>"
                val authResponse = loginAs(TestData.testUser1)

                // when
                val event = createEvent(authResponse, TestData.createEventRequest(title = title))

                // then
                assertEquals(event.id, event.vanityUrl)
            }

            @ParameterizedTest
            @MethodSource("pl.newbies.event.EventTest#vanityUrlTestCases")
            fun `should generate valid vanityUrl when requested with specified title`(
                case: VanityUrlTestCase
            ) = withAres {
                // given
                clearTable("Events")
                val authResponse = loginAs(TestData.testUser1)
                val request = TestData.createEventRequest(title = case.eventTitle)

                // when
                val event = createEvent(authResponse, request)

                // then
                assertEquals("${case.expectedVanityUrlStartsWith}-${event.id}", event.vanityUrl)
                assertTrue(event.vanityUrl.endsWith(event.id), "VanityUrl should end with event id")
            }
        }
    }

    @Nested
    inner class Put {
        @Test
        fun `should replace old data when called`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse = authResponse)
            val body = TestData.createEventRequest(title = "New longer Title")

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

        @Test
        fun `should replace old vanityUrl when called`() = withAres {
            // given
            clearTable("Events")
            val firstTitle = "firsttitlevanity"
            val secondTitle = "secondtitlevanity"
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse = authResponse, TestData.createEventRequest(title = firstTitle))
            assertEquals(firstTitle, event.vanityUrl.substringBeforeLast("-"))

            // when
            val response = httpClient.put("api/v1/events/${event.id}") {
                setBody(TestData.createEventRequest(title = secondTitle))
                contentType(ContentType.Application.Json)
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val updatedEvent = response.body<EventResponse>()
            assertNotEquals(event.vanityUrl, updatedEvent.vanityUrl)
        }

        @Test
        fun `should return 400 when called with invalid data`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse = authResponse)
            val body = """
                {}
            """.trimIndent()

            // when
            val response = httpClient.put("api/v1/events/${event.id}") {
                setBody(Json.parseToJsonElement(body))
                contentType(ContentType.Application.Json)
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

        @Test
        fun `should return 401 when called without authentication`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse = authResponse)

            // when
            val response = httpClient.put("api/v1/events/${event.id}") {
                setBody(TestData.createEventRequest(title = "New longer Title"))
                contentType(ContentType.Application.Json)
            }

            // then
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        @Test
        fun `should return 404 when called with not existing id`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val randomId = nanoId()

            // when
            val response = httpClient.put("api/v1/events/$randomId") {
                setBody(TestData.createEventRequest(title = "New longer Title"))
                contentType(ContentType.Application.Json)
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.NotFound, response.status)
        }

        @Test
        fun `should return 403 when called by another user`() = withAres {
            // given
            val firstAuthResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse = firstAuthResponse)
            val secondAuthResponse = loginAs(TestData.testUser2)

            // when
            val response = httpClient.delete("api/v1/events/${event.id}") {
                setBody(TestData.createEventRequest(title = "New longer Title"))
                contentType(ContentType.Application.Json)
                bearerAuth(secondAuthResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.Forbidden, response.status)
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
            val response = httpClient.delete("api/v1/events/${event.id}")

            // then
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        @Test
        fun `should return 404 when called with not existing id`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val randomId = nanoId()

            // when
            val response = httpClient.delete("api/v1/events/$randomId") {
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.NotFound, response.status)
        }

        @Test
        fun `should return 403 when called by another user`() = withAres {
            // given
            val firstAuthResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse = firstAuthResponse)
            val secondAuthResponse = loginAs(TestData.testUser2)

            // when
            val response = httpClient.delete("api/v1/events/${event.id}") {
                bearerAuth(secondAuthResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.Forbidden, response.status)
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
            val checkResponse = httpClient.get("api/v1/events/${event.id}")
            assertEquals(HttpStatusCode.NotFound, checkResponse.status)
        }

        @Test
        fun `should delete storage directory when delete called`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse)
            val fileResponse = addEventImage(
                authResponse = authResponse,
                eventId = event.id,
                imagePath = "images/newbies-logo.webp",
                contentType = "image/webp",
                fileName = "filename=newbies-logo.webp",
            )
            assertEquals(HttpStatusCode.OK, fileResponse.status)
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
    inner class PutVisibility {
        @Test
        fun `should return 401 when called without authentication`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse, visibility = Event.Visibility.PUBLIC)

            // when
            val response = httpClient.put("api/v1/events/${event.id}/visibility") {
                setBody(EventVisibilityRequest(Event.Visibility.PUBLIC))
                contentType(ContentType.Application.Json)
            }

            // then
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        @Test
        fun `should return 404 when called with not existing id`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)

            // when
            val response = httpClient.put("api/v1/events/someRandomId/visibility") {
                setBody(EventVisibilityRequest(Event.Visibility.PUBLIC))
                contentType(ContentType.Application.Json)
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.NotFound, response.status)
        }

        @Test
        fun `should return 403 when called by another user`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val secondAuthResponse = loginAs(TestData.testUser2)
            val event = createEvent(authResponse, visibility = Event.Visibility.PUBLIC)

            // when
            val response = httpClient.put("api/v1/events/${event.id}/visibility") {
                setBody(EventVisibilityRequest(Event.Visibility.PUBLIC))
                contentType(ContentType.Application.Json)
                bearerAuth(secondAuthResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

        @Test
        fun `should change visibility when called by an author`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse, visibility = Event.Visibility.PUBLIC)

            // when
            val response = changeVisibility(authResponse, event.id, Event.Visibility.PRIVATE)

            // then
            assertEquals(Event.Visibility.PRIVATE, response.visibility)
        }
    }

    @Nested
    inner class PutTheme {
        @Test
        fun `should return 401 when called without authentication`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse)
            val request = EventThemeRequest(primaryColor = "#58b5bf", secondaryColor = null)

            // when
            val response = httpClient.put("api/v1/events/${event.id}/theme") {
                setBody(request)
                contentType(ContentType.Application.Json)
            }

            // then
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        @Test
        fun `should return 404 when called with not existing id`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val request = EventThemeRequest(primaryColor = "#58b5bf", secondaryColor = null)

            // when
            val response = httpClient.put("api/v1/events/someRandomId/theme") {
                setBody(request)
                contentType(ContentType.Application.Json)
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.NotFound, response.status)
        }

        @Test
        fun `should return 403 when called by another user`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val secondAuthResponse = loginAs(TestData.testUser2)
            val event = createEvent(authResponse)
            val request = EventThemeRequest(primaryColor = "#58b5bf", secondaryColor = null)

            // when
            val response = httpClient.put("api/v1/events/${event.id}/theme") {
                setBody(request)
                contentType(ContentType.Application.Json)
                bearerAuth(secondAuthResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

        @Test
        fun `should change theme when called by an author`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse)
            val request = EventThemeRequest(primaryColor = "#58b5bf", secondaryColor = "#58b5bf")

            // when
            val response = httpClient.put("api/v1/events/${event.id}/theme") {
                setBody(request)
                contentType(ContentType.Application.Json)
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val updatedEvent = response.body<EventResponse>()
            assertNotEquals(event.theme.primaryColor, updatedEvent.theme.primaryColor)
            assertNotEquals(event.theme.secondaryColor, updatedEvent.theme.secondaryColor)
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
            val response = httpClient.put("api/v1/events/${event.id}/theme/image")

            // then
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        @Test
        fun `should return 404 when called with not existing id`() = withAres {
            // when
            val response = httpClient.put("api/v1/events/randomid/theme/image")

            // then
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        @Test
        fun `should 400 return when called with unsupported file`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse = authResponse)

            // when
            val response = addEventImage(
                authResponse = authResponse,
                eventId = event.id,
                imagePath = "images/newbies-logo.gif",
                contentType = "image/gif",
                fileName = "filename=newbies-logo.gif",
            )

            // then
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

        @Test
        fun `should 400 return when called without file`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse = authResponse)

            // when
            val response = httpClient.put("/api/v1/events/${event.id}/theme/image") {
                bearerAuth(authResponse.accessToken)
                setBody(MultiPartFormDataContent(parts = listOf()))
            }

            // then
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

        @Test
        fun `should return 403 when called by another user`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val secondAuthResponse = loginAs(TestData.testUser2)
            val event = createEvent(authResponse = authResponse)

            // when
            val response = addEventImage(
                authResponse = secondAuthResponse,
                eventId = event.id,
                imagePath = "images/newbies-logo.png",
                contentType = "image/png",
                fileName = "filename=newbies-logo.png",
            )

            // then
            assertEquals(HttpStatusCode.Forbidden, response.status)
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
            val response = addEventImage(
                authResponse = authResponse,
                eventId = event.id,
                imagePath = imagePath,
                contentType = contentType,
                fileName = fileName,
            )

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<FileUrlResponse>()
            assertEquals("http://localhost:80/api/v1/files/events/${event.id}/image.webp", responseBody.url)
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
            val response = httpClient.delete("api/v1/events/${event.id}/theme/image")

            // then
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        @Test
        fun `should return 404 when called with not existing id`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)

            // when
            val response = httpClient.delete("api/v1/events/someRandomId/theme/image") {
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.NotFound, response.status)
        }

        @Test
        fun `should return 403 when called by another user`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val secondAuthResponse = loginAs(TestData.testUser2)
            val event = createEvent(authResponse)
            val fileResponse = addEventImage(
                authResponse = authResponse,
                eventId = event.id,
                imagePath = "images/newbies-logo.png",
                contentType = "image/png",
                fileName = "filename=newbies-logo.png",
            )
            assertEquals(HttpStatusCode.OK, fileResponse.status)

            // when
            val response = httpClient.delete("api/v1/events/${event.id}/theme/image") {
                bearerAuth(secondAuthResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.Forbidden, response.status)
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
            val fileResponse = addEventImage(
                authResponse = authResponse,
                eventId = event.id,
                imagePath = "images/newbies-logo.png",
                contentType = "image/png",
                fileName = "filename=newbies-logo.png",
            )
            assertEquals(HttpStatusCode.OK, fileResponse.status)
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

    companion object {
        private val ALL_VISIBILITIES = listOf(
            Event.Visibility.PUBLIC,
            Event.Visibility.INVISIBLE,
            Event.Visibility.PRIVATE,
        )

        private suspend fun ApplicationTestBuilder.prepareFilterTestEvents(authResponse: AuthResponse) = buildList {
            add(createEvent(authResponse, visibility = Event.Visibility.PUBLIC))
            add(createEvent(authResponse, visibility = Event.Visibility.INVISIBLE))
            add(createEvent(authResponse, visibility = Event.Visibility.PRIVATE))
        }

        @JvmStatic
        fun vanityUrlTestCases() = listOf(
            VanityUrlTestCase("Jak skutecznie japko", "jak-skutecznie-japko"),
            VanityUrlTestCase("spaceonlastchar   ", "spaceonlastchar"),
            VanityUrlTestCase("illegallastchar   <", "illegallastchar"),
            VanityUrlTestCase("123 - testowa N4zwa", "123-testowa-n4zwa"),
            VanityUrlTestCase("345-testowa N4zwa", "345testowa-n4zwa"),
            VanityUrlTestCase("testowe  wydarzenie", "testowe-wydarzenie"),
            VanityUrlTestCase("somerandomwithoutspaces", "somerandomwithoutspaces"),
            VanityUrlTestCase("/to/do k,o'm`entarz", "todo-komentarz"),
            VanityUrlTestCase("ąąłłó óććęę", "aallo-occee"),
            VanityUrlTestCase("<><><bruh><><>", "bruh"),
        )

        @JvmStatic
        fun filterTestCases() = listOf(
            FilterTestCase(
                requester = EventRequester.AUTHOR,
                filter = EventFilter(visibilityIn = ALL_VISIBILITIES),
                expectedSize = 3,
                expectedVisibilities = ALL_VISIBILITIES,
            ),
            FilterTestCase(
                requester = EventRequester.AUTHOR,
                filter = EventFilter(authorId = "someRandomId", visibilityIn = ALL_VISIBILITIES),
                expectedSize = 0,
                expectedVisibilities = ALL_VISIBILITIES,
            ),
            FilterTestCase(
                requester = EventRequester.AUTHOR,
                filter = EventFilter(visibilityIn = listOf(Event.Visibility.PRIVATE, Event.Visibility.INVISIBLE)),
                expectedSize = 2,
                expectedVisibilities = listOf(Event.Visibility.PRIVATE, Event.Visibility.INVISIBLE),
            ),
            FilterTestCase(
                requester = EventRequester.AUTHOR,
                filter = EventFilter(visibilityIn = listOf(Event.Visibility.PUBLIC)),
                expectedSize = 1,
                expectedVisibilities = listOf(Event.Visibility.PUBLIC),
            ),
            FilterTestCase(
                requester = EventRequester.ANOTHER_USER,
                filter = EventFilter(visibilityIn = ALL_VISIBILITIES),
                expectedSize = 1,
                expectedVisibilities = listOf(Event.Visibility.PUBLIC),
            ),
            FilterTestCase(
                requester = EventRequester.ANOTHER_USER,
                filter = EventFilter(visibilityIn = listOf(Event.Visibility.PRIVATE)),
                expectedSize = 0,
                expectedVisibilities = listOf(Event.Visibility.PUBLIC),
            ),
            FilterTestCase(
                requester = EventRequester.UNAUTHORIZED,
                filter = EventFilter(visibilityIn = ALL_VISIBILITIES),
                expectedSize = 1,
                expectedVisibilities = listOf(Event.Visibility.PUBLIC),
            ),
        )
    }
}

data class FilterTestCase(
    val requester: EventRequester,
    val filter: EventFilter,
    val expectedSize: Int,
    val expectedVisibilities: List<Event.Visibility>
)

data class VanityUrlTestCase(
    val eventTitle: String,
    val expectedVanityUrlStartsWith: String,
)

enum class EventRequester {
    AUTHOR,
    ANOTHER_USER,
    UNAUTHORIZED,
}