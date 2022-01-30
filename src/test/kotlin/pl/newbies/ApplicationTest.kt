package pl.newbies

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pl.newbies.util.IntegrationTest
import pl.newbies.util.pongModule

class ApplicationTest : IntegrationTest() {

    @Test
    fun `should load all test modules`() = withAres {
        // given
        application { pongModule() }

        // when
        val response = client.get("/ping")

        // then
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("pong", response.bodyAsText())
    }
}