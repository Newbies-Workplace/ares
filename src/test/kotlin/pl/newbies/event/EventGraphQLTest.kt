package pl.newbies.event

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pl.newbies.generated.EventsList
import pl.newbies.generated.eventslist.EventResponse
import pl.newbies.util.IntegrationTest
import pl.newbies.util.graphQLClient

class EventGraphQLTest : IntegrationTest() {


    @Test
    fun `test something`() = withAres {
        // given
        clearTable("Events")

        // when
        val result = graphQLClient.execute(EventsList(EventsList.Variables()))

        // then
        assertEquals(emptyList<EventResponse>(), result.data?.events)
    }
}
