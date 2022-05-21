package pl.newbies.metrics

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.newbies.util.IntegrationTest
import pl.newbies.util.httpClient

class MetricsTest : IntegrationTest() {

    @Test
    fun `should return metrics when called`() = withAres {
        // given

        // when
        val response = httpClient.get("/metrics")

        // then
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().isNotEmpty(), "Metrics content should not be empty")
    }
}