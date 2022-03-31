package pl.newbies.storage

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.newbies.util.*

class StorageTest : IntegrationTest() {

    @Nested
    inner class GetFile {
        @Test
        fun `should return file when requested`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val lecture = createLecture(authResponse)
            val url = addLectureImage(
                authResponse = authResponse,
                lectureId = lecture.id,
                imagePath = "images/newbies-logo.png",
                contentType = "image/png",
                fileName = "filename=newbies-logo.png",
            ).url.substringAfter("/api/")

            // when
            val response = httpClient.get("/api/$url")

            // then
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ContentType("image", "webp"), response.contentType())
        }

        @Test
        fun `should return 404 when file does not exists`() = withAres {
            // when
            val exception = assertThrows<ClientRequestException> {
                httpClient.get("api/v1/files/lecture/nonexisting.jpg")
            }

            // then
            assertEquals(HttpStatusCode.NotFound, exception.response.status)
        }
    }
}