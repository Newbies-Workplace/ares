package pl.newbies.lecture

import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.datetime.Instant
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import pl.newbies.common.nanoId
import pl.newbies.event.application.model.TimeFrameRequest
import pl.newbies.lecture.application.model.LectureFilter
import pl.newbies.lecture.application.model.LectureResponse
import pl.newbies.plugins.defaultJson
import pl.newbies.util.*

class LectureTest : IntegrationTest() {
    @Nested
    inner class GetByEvent {

        @Test
        fun `should return 400 when no filter provided`() = withAres {
            // when
            val response = httpClient.get("api/v1/lectures")

            // then
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

        @Test
        fun `should return empty list when there is no event with given id`() = withAres {
            // given
            val eventId = nanoId()

            // when
            val response = httpClient.get("api/v1/lectures") {
                parameter(
                    "filter",
                    defaultJson.encodeToJsonElement(
                        LectureFilter(eventId = eventId)
                    )
                )
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<List<LectureResponse>>()
            assertEquals(emptyList<LectureResponse>(), responseBody)
        }

        @Test
        fun `should return empty list when there are no event lectures`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse)

            // when
            val response = httpClient.get("api/v1/lectures") {
                parameter(
                    "filter",
                    defaultJson.encodeToJsonElement(
                        LectureFilter(eventId = event.id)
                    )
                )
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<List<LectureResponse>>()
            assertEquals(emptyList<LectureResponse>(), responseBody)
        }

        @Test
        fun `should return event lecture list when called with proper event`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse)
            val lectures = buildList {
                repeat(3) { add(createLecture(authResponse, event.id)) }
            }

            // when
            val response = httpClient.get("api/v1/lectures") {
                parameter(
                    "filter",
                    defaultJson.encodeToJsonElement(
                        LectureFilter(eventId = event.id)
                    )
                )
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<List<LectureResponse>>()
            assertEquals(lectures.size, responseBody.size)
        }
    }

    @Nested
    inner class GetById {
        @Test
        fun `should return 404 when lecture does not exists`() = withAres {
            // given
            val lectureId = nanoId()

            // when
            val response = httpClient.get("api/v1/lectures/$lectureId")

            // then
            assertEquals(HttpStatusCode.NotFound, response.status)
        }

        @Test
        fun `should return lecture when called`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse)
            val lecture = createLecture(authResponse, event.id)

            // when
            val response = httpClient.get("api/v1/lectures/${lecture.id}")

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<LectureResponse>()
            assertEquals(lecture.id, responseBody.id)
        }
    }

    @Nested
    inner class Post {

        @ParameterizedTest(name = "[{index}] ({0}) event: {1} to {2}, lecture: {3} to {4}")
        @CsvSource(
            value = [
                "400,10,20,9,20",
                "400,10,11,10,12",
                "201,10,12,10,12",
            ]
        )
        fun `should return 400 when event time frame out of bounds`(
            status: Int,
            eventStart: Long,
            eventFinish: Long,
            lectureStart: Long,
            lectureFinish: Long,
        ) = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val eventRequest = TestData.createEventRequest(
                timeFrameRequest = TimeFrameRequest(
                    startDate = Instant.fromEpochMilliseconds(eventStart),
                    finishDate = Instant.fromEpochMilliseconds(eventFinish)
                )
            )
            val lectureRequest = TestData.createLectureRequest(
                timeFrameRequest = TimeFrameRequest(
                    startDate = Instant.fromEpochMilliseconds(lectureStart),
                    finishDate = Instant.fromEpochMilliseconds(lectureFinish)
                )
            )
            val event = createEvent(authResponse, eventRequest)

            // when
            val response = httpClient.post("api/v1/events/${event.id}/lectures") {
                setBody(lectureRequest)
                contentType(ContentType.Application.Json)
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.fromValue(status), response.status)
        }

        @Test
        fun `should return created lecture when called`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse)
            val request = TestData.createLectureRequest()

            // when
            val response = createLecture(authResponse, event.id)

            // then
            assertEquals(request.title, response.title)
            assertEquals(request.description, response.description)
            assertEquals(request.timeFrame.startDate, response.timeFrame.startDate)
            assertEquals(request.timeFrame.finishDate, response.timeFrame.finishDate)
        }

        @Test
        fun `should return lecture with speaker when called with speaker`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse)
            val request = TestData.createLectureRequest(
                speakerIds = listOf(authResponse.user.id)
            )

            // when
            val lecture = createLecture(authResponse, event.id, request)

            // then
            assertEquals(1, lecture.speakers.size)
            assertEquals(authResponse.user.id, lecture.speakers[0].id)
        }

        @Test
        fun `should return 401 when called without authentication`() = withAres {
            // when
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse)
            val response = httpClient.post("api/v1/events/${event.id}/lectures") {
                setBody(TestData.createLectureRequest())
                contentType(ContentType.Application.Json)
            }

            // then
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        @Test
        fun `should return 403 when called by another user`() = withAres {
            // given
            val firstAuthResponse = loginAs(TestData.testUser1)
            val event = createEvent(firstAuthResponse)
            val secondAuthResponse = loginAs(TestData.testUser2)

            // when
            val response = httpClient.post("api/v1/events/${event.id}/lectures") {
                setBody(TestData.createLectureRequest())
                contentType(ContentType.Application.Json)
                bearerAuth(secondAuthResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }
    }

    @Nested
    inner class Put {

        @Test
        fun `should return 401 when called without authentication`() = withAres {
            // when
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse)
            val lecture = createLecture(authResponse, event.id)
            val response = httpClient.put("api/v1/lectures/${lecture.id}") {
                setBody(TestData.createLectureRequest())
                contentType(ContentType.Application.Json)
            }

            // then
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        @Test
        fun `should return 403 when called by another user`() = withAres {
            // given
            val firstAuthResponse = loginAs(TestData.testUser1)
            val secondAuthResponse = loginAs(TestData.testUser2)
            val event = createEvent(firstAuthResponse)
            val lecture = createLecture(firstAuthResponse, event.id)

            // when
            val response = httpClient.put("api/v1/lectures/${lecture.id}") {
                setBody(TestData.createLectureRequest())
                contentType(ContentType.Application.Json)
                bearerAuth(secondAuthResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

        @Test
        fun `should update when called by author`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse)
            val lecture = createLecture(authResponse, event.id)
            val newTitle = "New lecture title"

            // when
            val response = httpClient.put("api/v1/lectures/${lecture.id}") {
                setBody(TestData.createLectureRequest(title = newTitle))
                contentType(ContentType.Application.Json)
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<LectureResponse>()
            assertNotEquals(lecture.updateDate, responseBody.updateDate)
            assertNotEquals(lecture.title, responseBody.title)
            assertEquals(newTitle, responseBody.title)
        }

        @Test
        fun `should update when called by speaker`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val speakerAuthResponse = loginAs(TestData.testUser2)
            val event = createEvent(authResponse)
            val lecture = createLecture(
                authResponse = authResponse,
                eventId = event.id,
                request = TestData.createLectureRequest(
                    speakerIds = listOf(speakerAuthResponse.user.id)
                )
            )
            val newTitle = "New lecture title"

            // when
            val response = httpClient.put("api/v1/lectures/${lecture.id}") {
                setBody(TestData.createLectureRequest(title = newTitle))
                contentType(ContentType.Application.Json)
                bearerAuth(speakerAuthResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<LectureResponse>()
            assertNotEquals(lecture.updateDate, responseBody.updateDate)
            assertNotEquals(lecture.title, responseBody.title)
            assertEquals(newTitle, responseBody.title)
        }

        @Test
        fun `should replace lecture speakers with new ones when called`() = withAres {
            // given
            val firstAuthResponse = loginAs(TestData.testUser1)
            val secondAuthResponse = loginAs(TestData.testUser2)
            val event = createEvent(firstAuthResponse)
            val lecture = createLecture(
                authResponse = firstAuthResponse,
                eventId = event.id,
                request = TestData.createLectureRequest(
                    speakerIds = listOf(firstAuthResponse.user.id)
                )
            )

            // when
            val response = httpClient.put("api/v1/lectures/${lecture.id}") {
                setBody(
                    TestData.createLectureRequest(
                        speakerIds = listOf(secondAuthResponse.user.id)
                    )
                )
                contentType(ContentType.Application.Json)
                bearerAuth(firstAuthResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<LectureResponse>()
            assertEquals(1, responseBody.speakers.size)
            assertNotEquals(lecture.speakers[0], responseBody.speakers[0])
            assertEquals(secondAuthResponse.user, responseBody.speakers[0])
        }

        @Test
        fun `should remove lecture speakers when called with empty speaker list`() = withAres {
            // given
            val firstAuthResponse = loginAs(TestData.testUser1)
            val secondAuthResponse = loginAs(TestData.testUser2)
            val event = createEvent(firstAuthResponse)
            val lecture = createLecture(
                authResponse = firstAuthResponse,
                eventId = event.id,
                request = TestData.createLectureRequest(
                    speakerIds = listOf(
                        firstAuthResponse.user.id,
                        secondAuthResponse.user.id,
                    )
                )
            )

            // when
            val response = httpClient.put("api/v1/lectures/${lecture.id}") {
                setBody(TestData.createLectureRequest(speakerIds = emptyList()))
                contentType(ContentType.Application.Json)
                bearerAuth(firstAuthResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<LectureResponse>()
            assertEquals(0, responseBody.speakers.size)
        }
    }

    @Nested
    inner class Delete {

        @Test
        fun `should return 401 when called without authentication`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse)
            val lecture = createLecture(authResponse, event.id)

            // when
            val response = httpClient.delete("api/v1/lectures/${lecture.id}")

            // then
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        @Test
        fun `should return 404 when called with non existing lecture id`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val lectureId = nanoId()

            // when
            val response = httpClient.delete("api/v1/lectures/$lectureId") {
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.NotFound, response.status)
        }

        @Test
        fun `should delete when called by author`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse)
            val lecture = createLecture(authResponse, event.id)

            // when
            val response = httpClient.delete("api/v1/lectures/${lecture.id}") {
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val deletedResponse = httpClient.get("api/v1/lectures/${lecture.id}")
            assertEquals(HttpStatusCode.NotFound, deletedResponse.status)
        }

        @Test
        fun `should delete when called by speaker`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val speakerAuthResponse = loginAs(TestData.testUser2)
            val event = createEvent(authResponse)
            val lecture = createLecture(
                authResponse = authResponse,
                eventId = event.id,
                request = TestData.createLectureRequest(
                    speakerIds = listOf(speakerAuthResponse.user.id)
                )
            )

            // when
            val response = httpClient.delete("api/v1/lectures/${lecture.id}") {
                bearerAuth(speakerAuthResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val deletedResponse = httpClient.get("api/v1/lectures/${lecture.id}")
            assertEquals(HttpStatusCode.NotFound, deletedResponse.status)
        }
    }
}