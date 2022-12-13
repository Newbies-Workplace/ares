package pl.newbies.user

import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import pl.newbies.common.nanoId
import pl.newbies.storage.application.model.FileUrlResponse
import pl.newbies.user.application.model.ContactRequest
import pl.newbies.user.application.model.UserRequest
import pl.newbies.user.application.model.UserResponse
import pl.newbies.util.*

class UserTest : IntegrationTest() {

    @Nested
    inner class GetById {
        @Test
        fun `should return user that exists in database`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser2) // create user
            val id = authResponse.getUserId()

            // when
            val response = httpClient.get("api/v1/users/$id")

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<UserResponse>()
            assertEquals(id, responseBody.id)
        }

        @Test
        fun `should return 404 when getting user that does not exists`() = withAres {
            // given
            val randomId = nanoId()

            // when
            val response = httpClient.get("api/v1/users/$randomId")

            // then
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Nested
    inner class GetMe {
        @Test
        fun `should return my user when called with authentication`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser2)
            val id = authResponse.getUserId()

            // when
            val response = httpClient.get("api/v1/users/@me") {
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<UserResponse>()
            assertEquals(id, responseBody.id)
        }

        @Test
        fun `should return 401 when called without authentication`() = withAres {
            // when
            val response = httpClient.get("api/v1/users/@me")

            // then
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Nested
    inner class Patch {
        @Test
        fun `should patch values when called with valid payload`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser2)
            val id = authResponse.getUserId()
            val body = """
                {
                "description":"newPatchedDescription",
                "nickname":"newPatchedNickname",
                "contact":{
                    "linkedin":"newPatchedLinkedIn"
                }
                }
                """.trimIndent()

            // when
            val response = httpClient.patch("api/v1/users/@me") {
                bearerAuth(authResponse.accessToken)
                setBody(Json.parseToJsonElement(body))
                contentType(ContentType.Application.Json)
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<UserResponse>()
            assertEquals(id, responseBody.id)
            assertEquals("newPatchedDescription", responseBody.description)
            assertEquals("newPatchedNickname", responseBody.nickname)
            assertEquals("newPatchedLinkedIn", responseBody.contact.linkedin)
        }

        @Test
        fun `should return 400 when called with invalid payload`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser2)
            val body = """{"description":{"invalid":true}}"""

            // when
            val response = httpClient.patch("api/v1/users/@me") {
                bearerAuth(authResponse.accessToken)
                setBody(Json.parseToJsonElement(body))
                contentType(ContentType.Application.Json)
            }

            // then
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

        @Test
        fun `should return 401 when called without authentication`() = withAres {
            // when
            val response = httpClient.patch("api/v1/users/@me")

            // then
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Nested
    inner class Put {
        @Test
        fun `should replace user when called with new data`() = withAres {
            // given
            val auth = loginAs(TestData.testUser2)
            val body = UserRequest(
                nickname = "newNickname",
                description = "newDescription",
                contact = ContactRequest(
                    github = "newGithub",
                    linkedin = "newLinkedin",
                    mail = null,
                    twitter = null,
                )
            )

            // when
            val response = httpClient.put("api/v1/users/@me") {
                bearerAuth(auth.accessToken)
                setBody(body)
                contentType(ContentType.Application.Json)
            }

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<UserResponse>()
            assertEquals(body.description, responseBody.description)
            assertEquals(body.nickname, responseBody.nickname)
            assertEquals(body.contact.github, responseBody.contact.github)
            assertEquals(body.contact.linkedin, responseBody.contact.linkedin)
            assertNull(responseBody.contact.mail)
            assertNull(responseBody.contact.twitter)
        }

        @Test
        fun `should return 400 when called with invalid payload`() = withAres {
            // given
            val auth = loginAs(TestData.testUser2)
            val body = Json.parseToJsonElement("{\"nickname\":\"\"}")

            // when
            val response = httpClient.put("api/v1/users/@me") {
                bearerAuth(auth.accessToken)
                setBody(body)
                contentType(ContentType.Application.Json)
            }

            // then
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

        @Test
        fun `should return 401 when called without authentication`() = withAres {
            // when
            val response = httpClient.put("api/v1/users/@me")

            // then
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Nested
    inner class PutAvatar {
        @Test
        fun `should return 401 when called without authentication`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)

            // when
            val response = httpClient.put("api/v1/users/@me/avatar")

            // then
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        @Test
        fun `should return 400 when called with unsupported file`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)

            // when
            val response = addUserAvatar(
                authResponse = authResponse,
                imagePath = "images/newbies-logo.gif",
                contentType = "image/gif",
                fileName = "filename=newbies-logo.gif",
            )

            // then
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

        @Test
        fun `should return 400 when called without file`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)

            // when
            val response = httpClient.put("api/v1/users/@me/avatar") {
                bearerAuth(authResponse.accessToken)
                setBody(MultiPartFormDataContent(parts = listOf()))
            }

            // then
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

        @ParameterizedTest
        @CsvSource(
            value = [
                "images/newbies-logo.jpg,image/jpg,newbies-logo.jpg",
                "images/newbies-logo.png,image/png,newbies-logo.png",
                "images/newbies-logo.webp,image/webp,newbies-logo.webp",
            ]
        )
        fun `should create image on valid request`(
            imagePath: String,
            contentType: String,
            fileName: String,
        ) = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)

            // when
            val response = addUserAvatar(
                authResponse = authResponse,
                imagePath = imagePath,
                contentType = contentType,
                fileName = fileName,
            )

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.body<FileUrlResponse>()
            assertEquals("http://localhost:80/api/v1/files/users/${authResponse.getUserId()}/avatar.webp", responseBody.url)
            assertFileExists("users/${authResponse.getUserId()}/avatar.webp")
        }
    }

    @Nested
    inner class DeleteAvatar {
        @Test
        fun `should return 401 when called without authentication`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)

            // when
            val response = httpClient.delete("api/v1/users/@me/avatar")

            // then
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }
}