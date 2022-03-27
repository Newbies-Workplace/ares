package pl.newbies.lecture

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.*
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.*
import kotlinx.serialization.json.Json
import org.junit.Ignore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import pl.newbies.lecture.application.model.LectureResponse
import pl.newbies.storage.application.model.FileUrlResponse
import pl.newbies.tag.application.model.TagResponse
import pl.newbies.util.*
import java.util.*

class LectureTest : IntegrationTest() {

    @Nested
    inner class GetAll {
        @Test
        fun `should return empty list when there are no lectures`() = withAres {
            // given
            clearTable("Lectures")

            // when
            val response = httpClient.get("api/v1/lectures")

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(emptyList<TagResponse>(), response.body<List<TagResponse>>())
        }

        @Test
        fun `should return lectures when there are some`() = withAres {
            // given
            clearTable("Lectures")
            val authResponse = loginAs(TestData.testUser1)
            val createdLectures = buildList {
                repeat(2) { add(createLecture(authResponse)) }
            }

            // when
            val response = httpClient.get("api/v1/lectures")

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<List<LectureResponse>>().map { it.id }
            assertEquals(2, responseBody.size)
            assertTrue(createdLectures[0].id in responseBody)
            assertTrue(createdLectures[1].id in responseBody)
        }

        @Test
        fun `should return next page when there are enough items`() = withAres {
            // given
            clearTable("Lectures")
            val authResponse = loginAs(TestData.testUser1)
            val createdLectures = buildList {
                repeat(2) { add(createLecture(authResponse)) }
            }

            // when
            val response = httpClient.get("api/v1/lectures") {
                parameter("page", 2L)
                parameter("size", 1L)
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<List<LectureResponse>>().map { it.id }
            assertEquals(1, responseBody.size)
            assertTrue(createdLectures[1].id in responseBody)
        }

        @Test
        fun `should return empty list when empty page requested`() = withAres {
            // given
            clearTable("Lectures")
            val authResponse = loginAs(TestData.testUser1)
            repeat(2) {
                createLecture(authResponse)
            }

            // when
            val response = httpClient.get("api/v1/lectures") {
                parameter("page", 3L)
                parameter("size", 1L)
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<List<LectureResponse>>()
            assertEquals(emptyList<List<LectureResponse>>(), responseBody)
        }
    }

    @Nested
    inner class GetById {
        @Test
        fun `should return 404 when there is no lecture with given id`() = withAres {
            // given
            clearTable("Lectures")
            val randomId = UUID.randomUUID().toString()

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.get("api/v1/lectures/$randomId")
            }

            // then
            assertEquals(HttpStatusCode.NotFound, exception.response.status)
        }

        @Test
        fun `should return existing lecture when called with valid id`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val lecture = createLecture(authResponse)

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
        @Test
        fun `should return 401 when called without authentication`() = withAres {
            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.post("api/v1/lectures") {
                    setBody(TestData.createLectureRequest())
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
                httpClient.post("api/v1/lectures") {
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
            val lecture = createLecture(authResponse = authResponse)
            val body = TestData.createLectureRequest(title = "NewTitle")

            // when
            val response = httpClient.put("api/v1/lectures/${lecture.id}") {
                setBody(body)
                contentType(ContentType.Application.Json)
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<LectureResponse>()
            assertEquals(body.title, responseBody.title)
        }

        @Ignore
        @Test
        fun `should return 400 when called with invalid data`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val lecture = createLecture(authResponse = authResponse)
            val body = """
                {}
            """.trimIndent()

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.put("api/v1/lectures/${lecture.id}") {
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
            val lecture = createLecture(authResponse = authResponse)

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.put("api/v1/lectures/${lecture.id}") {
                    setBody(TestData.createLectureRequest(title = "NewTitle"))
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
            val randomId = UUID.randomUUID().toString()

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.put("api/v1/lectures/$randomId") {
                    setBody(TestData.createLectureRequest(title = "NewTitle"))
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
            val lecture = createLecture(authResponse = firstAuthResponse)
            val secondAuthResponse = loginAs(TestData.testUser2)

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.delete("api/v1/lectures/${lecture.id}") {
                    setBody(TestData.createLectureRequest(title = "NewTitle"))
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
            val lecture = createLecture(authResponse = authResponse)

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.delete("api/v1/lectures/${lecture.id}")
            }

            // then
            assertEquals(HttpStatusCode.Unauthorized, exception.response.status)
        }

        @Test
        fun `should return 404 when called with not existing id`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val randomId = UUID.randomUUID().toString()

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.delete("api/v1/lectures/$randomId") {
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
            val lecture = createLecture(authResponse = firstAuthResponse)
            val secondAuthResponse = loginAs(TestData.testUser2)

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.delete("api/v1/lectures/${lecture.id}") {
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
            val lecture = createLecture(authResponse)

            // when
            val response = httpClient.delete("api/v1/lectures/${lecture.id}") {
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val exception = assertThrows<ClientRequestException> {
                httpClient.get("api/v1/lectures/${lecture.id}")
            }
            assertEquals(HttpStatusCode.NotFound, exception.response.status)
        }

        //todo remove directory on delete
    }

    @Nested
    inner class PutThemeImage {
        @Test
        fun `should return 401 when called without authentication`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val lecture = createLecture(authResponse = authResponse)

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.put("api/v1/lectures/${lecture.id}/theme/image")
            }

            // then
            assertEquals(HttpStatusCode.Unauthorized, exception.response.status)
        }

        @Test
        fun `should return 404 when called with not existing id`() = withAres {
            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.put("api/v1/lectures/randomid/theme/image")
            }

            // then
            assertEquals(HttpStatusCode.Unauthorized, exception.response.status)
        }

        @Test
        fun `should 400 return when called with unsupported file`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val lecture = createLecture(authResponse = authResponse)

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.put("/api/v1/lectures/${lecture.id}/theme/image") {
                    bearerAuth(authResponse.accessToken)
                    setBody(MultiPartFormDataContent(
                        parts = formData {
                            append("image", getResourceFile("images/newbies-logo.gif").readBytes(), Headers.build {
                                append(HttpHeaders.ContentType, "image/gif")
                                append(HttpHeaders.ContentDisposition, "filename=newbies-logo.gif")
                            })
                        },
                    ))
                    onUpload { bytesSentTotal, contentLength ->
                        println("Sent $bytesSentTotal bytes from $contentLength")
                    }
                }
            }

            // then
            assertEquals(HttpStatusCode.BadRequest, exception.response.status)
        }

        @Test
        fun `should 400 return when called without file`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val lecture = createLecture(authResponse = authResponse)

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.put("/api/v1/lectures/${lecture.id}/theme/image") {
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
            val lecture = createLecture(authResponse = authResponse)

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.put("/api/v1/lectures/${lecture.id}/theme/image") {
                    bearerAuth(secondAuthResponse.accessToken)
                    setBody(MultiPartFormDataContent(
                        parts = formData {
                            append("image", getResourceFile("images/newbies-logo.png").readBytes(), Headers.build {
                                append(HttpHeaders.ContentType, "image/png")
                                append(HttpHeaders.ContentDisposition, "filename=newbies-logo.png")
                            })
                        },
                    ))
                    onUpload { bytesSentTotal, contentLength ->
                        println("Sent $bytesSentTotal bytes from $contentLength")
                    }
                }
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
            val lecture = createLecture(authResponse = authResponse)

            // when
            //todo extract to fun
            val response = httpClient.put("/api/v1/lectures/${lecture.id}/theme/image") {
                bearerAuth(authResponse.accessToken)
                setBody(MultiPartFormDataContent(
                    parts = formData {
                        append("image", getResourceFile(imagePath).readBytes(), Headers.build {
                            append(HttpHeaders.ContentType, contentType)
                            append(HttpHeaders.ContentDisposition, "filename=$fileName")
                        })
                    },
                ))
                onUpload { bytesSentTotal, contentLength ->
                    println("Sent $bytesSentTotal bytes from $contentLength")
                }
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<FileUrlResponse>()
            assertEquals(responseBody.url, "http://localhost:80/api/v1/files/lectures/${lecture.id}/image.webp")
            assertFileExists("lectures/${lecture.id}/image.webp")
        }
    }

    @Nested
    inner class DeleteThemeImage {
        @Test
        fun `should return 401 when called without authentication`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val lecture = createLecture(authResponse = authResponse)

            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.delete("api/v1/lectures/${lecture.id}/theme/image")
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
                httpClient.delete("api/v1/lectures/someRandomId/theme/image") {
                    bearerAuth(authResponse.accessToken)
                }
            }

            // then
            assertEquals(HttpStatusCode.NotFound, exception.response.status)
        }

        // todo 403 on no permission
        // todo ok when removing not existing image
        // todo ok when removed existing image
    }
}