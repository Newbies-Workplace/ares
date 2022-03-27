package pl.newbies.storage

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.newbies.util.IntegrationTest
import pl.newbies.util.httpClient

class StorageTest : IntegrationTest() {

    @Nested
    inner class GetFile {
        //todo get file test
//        @Test
//        fun `should return file when requested`() = withAres {
//            // given
//
//            // when
//            httpClient.get("api/v1/files/somefile.jpg")
//
//            // then
//        }

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