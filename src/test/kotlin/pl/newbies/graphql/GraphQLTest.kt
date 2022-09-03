package pl.newbies.graphql

import io.ktor.client.request.bearerAuth
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pl.newbies.event.application.model.TimeFrameRequest
import pl.newbies.event.domain.model.Event
import pl.newbies.generated.EventByIdQuery
import pl.newbies.generated.EventListQuery
import pl.newbies.util.*

class GraphQLTest : IntegrationTest() {

    @Test
    fun `should convert Instant scalar to string`() = withAres {
        // given
        val authResponse = loginAs(TestData.testUser1)
        val startDateString = "2023-04-01T12:00:00Z"
        val event = createEvent(
            authResponse = authResponse,
            request = TestData.createEventRequest(
                timeFrameRequest = TimeFrameRequest(
                    startDate = Instant.parse(startDateString)
                )
            ),
            visibility = Event.Visibility.PUBLIC,
        )

        // when
        val response = graphQLClient.execute(
            EventByIdQuery(
                EventByIdQuery.Variables(
                    id = event.id
                )
            )
        ) {
            bearerAuth(authResponse.accessToken)
        }

        // then
        val createdEvent = response.data?.event!!
        assertEquals(startDateString, createdEvent.timeFrame.startDate)
    }

    @Test
    fun `should return batched response when queried with multiple requests`() = withAres {
        // given
        clearTable("Events")
        val authResponse = loginAs(TestData.testUser1)
        val firstEvent = createEvent(authResponse)
        createEvent(authResponse)

        // when
        val response = graphQLClient.execute(
            listOf(
                EventByIdQuery(
                    EventByIdQuery.Variables(
                        id = firstEvent.id
                    )
                ),
                EventListQuery(
                    EventListQuery.Variables()
                )
            )
        )

        // then
        val event = (response[0].data as EventByIdQuery.Result).event
        val events = (response[1].data as EventListQuery.Result).events
        assertEquals(firstEvent.id, event.id)
        assertEquals(2, events.size)
    }
}