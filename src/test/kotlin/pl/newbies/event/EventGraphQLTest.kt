package pl.newbies.event

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import pl.newbies.common.nanoId
import pl.newbies.event.EventTest.Companion.prepareFilterTestEvents
import pl.newbies.event.application.model.EventFilter
import pl.newbies.event.domain.model.Event
import pl.newbies.generated.*
import pl.newbies.generated.enums.Visibility
import pl.newbies.generated.eventlistquery.EventResponse
import pl.newbies.generated.inputs.EventFilterInput
import pl.newbies.generated.inputs.EventThemeRequestInput
import pl.newbies.generated.inputs.EventVisibilityRequestInput
import pl.newbies.util.*

class EventGraphQLTest : IntegrationTest() {

    @Nested
    inner class DataLoaders {
        @Nested
        inner class Author {
            @Test
            fun `should return author for single event`() = withAres {
                // given
                val authResponse = loginAs(TestData.testUser1)
                val event = createEvent(authResponse)

                // when
                val response = graphQLClient.execute(
                    EventByIdQuery(
                        EventByIdQuery.Variables(
                            id = event.id,
                        )
                    )
                )

                // then
                assertNotNull(response.data?.event?.author)
                assertEquals(authResponse.user.nickname, response.data?.event?.author?.nickname)
            }

            @Test
            fun `should return author for event list`() = withAres {
                // given
                clearTable("Events")
                val authResponse = loginAs(TestData.testUser1)
                createEvent(authResponse)
                createEvent(authResponse)

                // when
                val response = graphQLClient.execute(
                    EventListQuery(
                        EventListQuery.Variables()
                    )
                )

                // then
                val event = response.data?.events?.firstOrNull()
                assertNotNull(event?.author)
                assertEquals(authResponse.user.nickname, event?.author?.nickname)
            }
        }

        @Nested
        inner class IsFollowed {
            @Test
            fun `should return isFollowed for single event when user is authorized`() = withAres {
                // given
                val authResponse = loginAs(TestData.testUser1)
                val event = createEvent(authResponse)

                // when
                val response = graphQLClient.execute(
                    EventWithFollowedByIdQuery(
                        EventWithFollowedByIdQuery.Variables(
                            id = event.id,
                        )
                    )
                ) {
                    bearerAuth(authResponse.accessToken)
                }

                // then
                assertNotNull(response.data?.event?.isFollowed)
            }

            @Test
            fun `should return null for single event when user is unauthorized`() = withAres {
                // given
                val authResponse = loginAs(TestData.testUser1)
                val event = createEvent(authResponse)

                // when
                val response = graphQLClient.execute(
                    EventWithFollowedByIdQuery(
                        EventWithFollowedByIdQuery.Variables(
                            id = event.id,
                        )
                    )
                )

                // then
                assertNull(response.data?.event?.isFollowed)
                assertNull(response.data)
                assertNotNull(response.errorAt("event"))
            }

            @Test
            fun `should return isFollowed for event list when user is authorized`() = withAres {
                // given
                clearTable("Events")
                val authResponse = loginAs(TestData.testUser1)
                createEvent(authResponse)
                createEvent(authResponse)

                // when
                val response = graphQLClient.execute(
                    EventListWithFollowedQuery(
                        EventListWithFollowedQuery.Variables()
                    )
                ) {
                    bearerAuth(authResponse.accessToken)
                }

                // then
                assertNotNull(response.data?.events?.firstOrNull()?.isFollowed)
            }

            @Test
            fun `should return null for event list when user is unauthorized`() = withAres {
                // given
                clearTable("Events")
                val authResponse = loginAs(TestData.testUser1)
                createEvent(authResponse)

                // when
                val response = graphQLClient.execute(
                    EventListWithFollowedQuery(
                        EventListWithFollowedQuery.Variables()
                    )
                )

                // then
                assertNull(response.data?.events?.firstOrNull()?.isFollowed)
                assertNull(response.data)
                assertNotNull(response.errorAt("events"))
            }
        }
    }

    @Nested
    inner class EventList {

        @Test
        fun `should return empty list when there are no events`() = withAres {
            // given
            clearTable("Events")

            // when
            val response = graphQLClient.execute(
                EventListQuery(
                    EventListQuery.Variables()
                )
            )

            // then
            assertEquals(emptyList<EventResponse>(), response.data?.events)
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
            val response = graphQLClient.execute(
                EventListQuery(
                    EventListQuery.Variables()
                )
            )

            // then
            val responseBody = response.data?.events!!.map { it.id }
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
            val response = graphQLClient.execute(
                EventListQuery(
                    EventListQuery.Variables(
                        page = 2,
                        size = 1,
                    )
                )
            )

            // then
            val responseBody = response.data?.events!!.map { it.id }
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
            val response = graphQLClient.execute(
                EventListQuery(
                    EventListQuery.Variables(
                        page = 3,
                        size = 1,
                    )
                )
            )

            // then
            val responseBody = response.data?.events!!
            assertEquals(emptyList<EventResponse>(), responseBody)
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
            val response = graphQLClient.execute(
                EventListQuery(
                    EventListQuery.Variables()
                )
            )

            // then
            val responseBody = response.data?.events!!
            assertEquals(emptyList<EventResponse>(), responseBody)
        }

        @Nested
        inner class Filtered {

            @ParameterizedTest
            @MethodSource("pl.newbies.event.EventTest#filterTestCases")
            fun `should return expected visibilities when requested`(case: FilterTestCase) = withAres {
                // given
                clearTable("Events")
                val authResponse = loginAs(TestData.testUser1)
                val secondAuthResponse = loginAs(TestData.testUser2)
                prepareFilterTestEvents(authResponse)

                // when
                val response = graphQLClient.execute(
                    EventListQuery(
                        EventListQuery.Variables(
                            filter = case.filter.toInput()
                        )
                    )
                ) {
                    when (case.requester) {
                        EventRequester.AUTHOR -> bearerAuth(authResponse.accessToken)
                        EventRequester.ANOTHER_USER -> bearerAuth(secondAuthResponse.accessToken)
                        EventRequester.UNAUTHORIZED -> Unit
                    }
                }

                // then
                val body = response.data?.events!!
                assertEquals(case.expectedSize, body.size)
                assertTrue(body.all { Event.Visibility.valueOf(it.visibility.name) in case.expectedVisibilities })
            }
        }
    }

    @Nested
    inner class EventById {
        @Test
        fun `should return null when there is no event with given id`() = withAres {
            // given
            clearTable("Events")
            val randomId = nanoId()

            // when
            val response = graphQLClient.execute(
                EventByIdQuery(
                    EventByIdQuery.Variables(
                        id = randomId
                    )
                )
            )

            // then
            assertNull(response.data?.event)
            assertNotNull(response.errorAt("event"))
        }

        @Test
        fun `should return existing event when called with valid id`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse)

            // when
            val response = graphQLClient.execute(
                EventByIdQuery(
                    EventByIdQuery.Variables(
                        id = event.id
                    )
                )
            )

            // then
            val responseBody = response.data?.event!!
            assertEquals(event.id, responseBody.id)
        }

        @ParameterizedTest
        @CsvSource(
            value = [
                "false,PRIVATE,AUTHOR",
                "true,PRIVATE,ANOTHER_USER",
                "true,PRIVATE,UNAUTHORIZED",
                "false,INVISIBLE,AUTHOR",
                "false,INVISIBLE,ANOTHER_USER",
                "false,INVISIBLE,UNAUTHORIZED",
                "false,PUBLIC,AUTHOR",
                "false,PUBLIC,ANOTHER_USER",
                "false,PUBLIC,UNAUTHORIZED",
            ]
        )
        fun `should return correct status when event has specific visibility and requested by specific user`(
            isNull: Boolean,
            visibility: Event.Visibility,
            requester: EventRequester,
        ) = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val secondAuthResponse = loginAs(TestData.testUser2)
            val event = createEvent(authResponse, visibility = visibility)

            // when
            val response = graphQLClient.execute(
                EventByIdQuery(
                    EventByIdQuery.Variables(
                        id = event.id
                    )
                )
            ) {
                when (requester) {
                    EventRequester.AUTHOR -> bearerAuth(authResponse.accessToken)
                    EventRequester.ANOTHER_USER -> bearerAuth(secondAuthResponse.accessToken)
                    EventRequester.UNAUTHORIZED -> Unit
                }
            }

            // then
            assertEquals(isNull, response.data?.event == null)
        }
    }

    @Nested
    inner class CreateEvent {
        @Test
        fun `should return null when called without authentication`() = withAres {
            // when
            val response = graphQLClient.execute(
                CreateEventMutation(
                    CreateEventMutation.Variables(
                        request = TestData.createEventRequestInput()
                    )
                )
            )

            // then
            assertNull(response.data?.createEvent)
            assertNotNull(response.errorAt("createEvent"))
        }

        @Test
        fun `should return null when called with invalid data`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)

            // when
            val response = graphQLClient.execute(
                CreateEventMutation(
                    CreateEventMutation.Variables(
                        request = TestData.createEventRequestInput(title = "")
                    )
                )
            ) {
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertNull(response.data?.createEvent)
            assertNotNull(response.errorAt("createEvent"))
        }

        @Test
        fun `should create event when request contains emoji`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val title = "Some title with emojis \uD83C\uDF55"
            val request = TestData.createEventRequestInput(title = title)

            // when
            val response = graphQLClient.execute(
                CreateEventMutation(
                    CreateEventMutation.Variables(
                        request = request
                    )
                )
            ) {
                bearerAuth(authResponse.accessToken)
            }

            // then
            val event = response.data?.createEvent!!
            assertNotNull(event.id)
            assertEquals(title, event.title)
            assertTrue(event.title.contains("🍕"))
        }

        @Test
        fun `should create event when called with valid request`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)

            // when
            val response = graphQLClient.execute(
                CreateEventMutation(
                    CreateEventMutation.Variables(
                        request = TestData.createEventRequestInput()
                    )
                )
            ) {
                bearerAuth(authResponse.accessToken)
            }

            // then
            val event = response.data?.createEvent!!
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
                val response = graphQLClient.execute(
                    CreateEventMutation(
                        CreateEventMutation.Variables(
                            request = TestData.createEventRequestInput(title = title)
                        )
                    )
                ) {
                    bearerAuth(authResponse.accessToken)
                }

                // then
                val secondEvent = response.data?.createEvent!!
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
                val response = graphQLClient.execute(
                    CreateEventMutation(
                        CreateEventMutation.Variables(
                            request = TestData.createEventRequestInput(title = title)
                        )
                    )
                ) {
                    bearerAuth(authResponse.accessToken)
                }

                // then
                val event = response.data?.createEvent!!
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
                val request = TestData.createEventRequestInput(title = case.eventTitle)

                // when
                val response = graphQLClient.execute(
                    CreateEventMutation(
                        CreateEventMutation.Variables(
                            request = request
                        )
                    )
                ) {
                    bearerAuth(authResponse.accessToken)
                }

                // then
                val event = response.data?.createEvent!!
                assertEquals("${case.expectedVanityUrlStartsWith}-${event.id}", event.vanityUrl)
                assertTrue(event.vanityUrl.endsWith(event.id), "VanityUrl should end with event id")
            }
        }
    }

    @Nested
    inner class ReplaceEvent {
        @Test
        fun `should replace old data when called`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse = authResponse)
            val request = TestData.createEventRequestInput(title = "New longer Title")

            // when
            val response = graphQLClient.execute(
                ReplaceEventMutation(
                    ReplaceEventMutation.Variables(
                        id = event.id,
                        request = request,
                    )
                )
            ) {
                bearerAuth(authResponse.accessToken)
            }

            // then
            val replacedEvent = response.data?.replaceEvent!!
            assertEquals(request.title, replacedEvent.title)
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
            val response = graphQLClient.execute(
                ReplaceEventMutation(
                    ReplaceEventMutation.Variables(
                        id = event.id,
                        request = TestData.createEventRequestInput(title = secondTitle),
                    )
                )
            ) {
                bearerAuth(authResponse.accessToken)
            }

            // then
            val replacedEvent = response.data?.replaceEvent!!
            assertNotEquals(event.vanityUrl, replacedEvent.vanityUrl)
        }

        @Test
        fun `should return null when called with invalid data`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse = authResponse)

            // when
            val response = graphQLClient.execute(
                ReplaceEventMutation(
                    ReplaceEventMutation.Variables(
                        id = event.id,
                        request = TestData.createEventRequestInput(title = ""),
                    )
                )
            ) {
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertNull(response.data?.replaceEvent)
            assertNotNull(response.errorAt("replaceEvent"))
        }

        @Test
        fun `should return null when called without authentication`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse = authResponse)

            // when
            val response = graphQLClient.execute(
                ReplaceEventMutation(
                    ReplaceEventMutation.Variables(
                        id = event.id,
                        request = TestData.createEventRequestInput(),
                    )
                )
            )

            // then
            assertNull(response.data?.replaceEvent)
            assertNotNull(response.errorAt("replaceEvent"))
        }

        @Test
        fun `should return null when called with not existing id`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val randomId = nanoId()

            // when
            val response = graphQLClient.execute(
                ReplaceEventMutation(
                    ReplaceEventMutation.Variables(
                        id = randomId,
                        request = TestData.createEventRequestInput(),
                    )
                )
            ) {
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertNull(response.data?.replaceEvent)
            assertNotNull(response.errorAt("replaceEvent"))
        }

        @Test
        fun `should return null when called by another user`() = withAres {
            // given
            val firstAuthResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse = firstAuthResponse)
            val secondAuthResponse = loginAs(TestData.testUser2)

            // when
            val response = graphQLClient.execute(
                ReplaceEventMutation(
                    ReplaceEventMutation.Variables(
                        id = event.id,
                        request = TestData.createEventRequestInput(title = "New longer Title"),
                    )
                )
            ) {
                bearerAuth(secondAuthResponse.accessToken)
            }

            // then
            assertNull(response.data?.replaceEvent)
            assertNotNull(response.errorAt("replaceEvent"))
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `should return null when called without authentication`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse = authResponse)

            // when
            val response = graphQLClient.execute(
                DeleteEventMutation(
                    DeleteEventMutation.Variables(
                        id = event.id,
                    )
                )
            )

            // then
            assertNull(response.data?.deleteEvent)
            assertNotNull(response.errorAt("deleteEvent"))
        }

        @Test
        fun `should return null when called with not existing id`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val randomId = nanoId()

            // when
            val response = graphQLClient.execute(
                DeleteEventMutation(
                    DeleteEventMutation.Variables(
                        id = randomId,
                    )
                )
            ) {
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertNull(response.data?.deleteEvent)
            assertNotNull(response.errorAt("deleteEvent"))
        }

        @Test
        fun `should return null when called by another user`() = withAres {
            // given
            val firstAuthResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse = firstAuthResponse)
            val secondAuthResponse = loginAs(TestData.testUser2)

            // when
            val response = graphQLClient.execute(
                DeleteEventMutation(
                    DeleteEventMutation.Variables(
                        id = event.id,
                    )
                )
            ) {
                bearerAuth(secondAuthResponse.accessToken)
            }

            // then
            assertNull(response.data?.deleteEvent)
            assertNotNull(response.errorAt("deleteEvent"))
        }

        @Test
        fun `should delete when called by author`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse)

            // when
            val response = graphQLClient.execute(
                DeleteEventMutation(
                    DeleteEventMutation.Variables(
                        id = event.id,
                    )
                )
            ) {
                bearerAuth(authResponse.accessToken)
            }

            // then
            val deletedEvent = response.data?.deleteEvent!!
            assertTrue(deletedEvent)
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
            val response = graphQLClient.execute(
                DeleteEventMutation(
                    DeleteEventMutation.Variables(
                        id = event.id,
                    )
                )
            ) {
                bearerAuth(authResponse.accessToken)
            }

            // then
            val deletedEvent = response.data?.deleteEvent!!
            assertTrue(deletedEvent)
            assertFileNotExists("events/${event.id}")
        }
    }

    @Nested
    inner class ChangeEventVisibility {
        @Test
        fun `should return null when called without authentication`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse, visibility = Event.Visibility.PUBLIC)

            // when
            val response = graphQLClient.execute(
                ChangeEventVisibilityMutation(
                    ChangeEventVisibilityMutation.Variables(
                        id = event.id,
                        request = EventVisibilityRequestInput(Visibility.PUBLIC)
                    )
                )
            )

            // then
            assertNull(response.data?.changeEventVisibility)
            assertNotNull(response.errorAt("changeEventVisibility"))
        }

        @Test
        fun `should return null when called with not existing id`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val randomId = nanoId()

            // when
            val response = graphQLClient.execute(
                ChangeEventVisibilityMutation(
                    ChangeEventVisibilityMutation.Variables(
                        id = randomId,
                        request = EventVisibilityRequestInput(Visibility.PUBLIC)
                    )
                )
            ) {
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertNull(response.data?.changeEventVisibility)
            assertNotNull(response.errorAt("changeEventVisibility"))
        }

        @Test
        fun `should return null when called by another user`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val secondAuthResponse = loginAs(TestData.testUser2)
            val event = createEvent(authResponse, visibility = Event.Visibility.PUBLIC)

            // when
            val response = graphQLClient.execute(
                ChangeEventVisibilityMutation(
                    ChangeEventVisibilityMutation.Variables(
                        id = event.id,
                        request = EventVisibilityRequestInput(Visibility.PUBLIC)
                    )
                )
            ) {
                bearerAuth(secondAuthResponse.accessToken)
            }

            // then
            assertNull(response.data?.changeEventVisibility)
            assertNotNull(response.errorAt("changeEventVisibility"))
        }

        @Test
        fun `should change visibility when called by an author`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse, visibility = Event.Visibility.PUBLIC)

            // when
            val response = graphQLClient.execute(
                ChangeEventVisibilityMutation(
                    ChangeEventVisibilityMutation.Variables(
                        id = event.id,
                        request = EventVisibilityRequestInput(Visibility.PRIVATE)
                    )
                )
            ) {
                bearerAuth(authResponse.accessToken)
            }

            // then
            val updatedEvent = response.data?.changeEventVisibility!!
            assertEquals(Visibility.PRIVATE, updatedEvent.visibility)
            assertNotEquals(event.updateDate.toString(), updatedEvent.updateDate)
        }
    }

    @Nested
    inner class PutTheme {
        @Test
        fun `should return null when called without authentication`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse)
            val request = EventThemeRequestInput(primaryColor = "#58b5bf", secondaryColor = null)

            // when
            val response = graphQLClient.execute(
                ReplaceEventThemeMutation(
                    ReplaceEventThemeMutation.Variables(
                        id = event.id,
                        request = request
                    )
                )
            )

            // then
            assertNull(response.data?.replaceEventTheme)
            assertNotNull(response.errorAt("replaceEventTheme"))
        }

        @Test
        fun `should return null when called with not existing id`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val request = EventThemeRequestInput(primaryColor = "#58b5bf", secondaryColor = null)
            val randomId = nanoId()

            // when
            val response = graphQLClient.execute(
                ReplaceEventThemeMutation(
                    ReplaceEventThemeMutation.Variables(
                        id = randomId,
                        request = request
                    )
                )
            ) {
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertNull(response.data?.replaceEventTheme)
            assertNotNull(response.errorAt("replaceEventTheme"))
        }

        @Test
        fun `should return null when called by another user`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val secondAuthResponse = loginAs(TestData.testUser2)
            val event = createEvent(authResponse)
            val request = EventThemeRequestInput(primaryColor = "#58b5bf", secondaryColor = null)

            // when
            val response = graphQLClient.execute(
                ReplaceEventThemeMutation(
                    ReplaceEventThemeMutation.Variables(
                        id = event.id,
                        request = request
                    )
                )
            ) {
                bearerAuth(secondAuthResponse.accessToken)
            }

            // then
            assertNull(response.data?.replaceEventTheme)
            assertNotNull(response.errorAt("replaceEventTheme"))
        }

        @Test
        fun `should change theme when called by an author`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse)
            val request = EventThemeRequestInput(primaryColor = "#58b5bf", secondaryColor = "#58b5bf")

            // when
            val response = graphQLClient.execute(
                ReplaceEventThemeMutation(
                    ReplaceEventThemeMutation.Variables(
                        id = event.id,
                        request = request
                    )
                )
            ) {
                bearerAuth(authResponse.accessToken)
            }

            // then
            val updatedEvent = response.data?.replaceEventTheme!!
            assertNotEquals(event.updateDate.toString(), updatedEvent.updateDate)
            assertNotEquals(event.theme.primaryColor, updatedEvent.theme.primaryColor)
            assertNotEquals(event.theme.secondaryColor, updatedEvent.theme.secondaryColor)
        }
    }

    @Nested
    inner class FollowEvent {
        @Test
        fun `should return null when called by unauthorized user`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse)

            // when
            val response = graphQLClient.execute(
                FollowEventMutation(
                    FollowEventMutation.Variables(
                        id = event.id,
                    )
                )
            )

            // then
            assertNull(response.data?.followEvent)
            assertNull(response.data)
            assertNotNull(response.errorAt("followEvent"))
        }

        @Test
        fun `should follow when called by authorized user`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse)

            // when
            val response = graphQLClient.execute(
                FollowEventMutation(
                    FollowEventMutation.Variables(
                        id = event.id,
                    )
                )
            ) {
                bearerAuth(authResponse.accessToken)
            }

            // when
            assertTrue(response.data?.followEvent!!)
        }
    }
}

fun EventFilter.toInput() = EventFilterInput(
    authorId = authorId,
    visibilityIn = visibilityIn.map {
        Visibility.valueOf(it.name)
    },
)