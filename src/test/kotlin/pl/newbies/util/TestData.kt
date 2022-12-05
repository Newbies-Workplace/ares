package pl.newbies.util

import kotlinx.datetime.Instant
import pl.newbies.auth.application.model.GithubUser
import pl.newbies.event.application.model.AddressRequest
import pl.newbies.event.application.model.EventRequest
import pl.newbies.event.application.model.TimeFrameRequest
import pl.newbies.generated.inputs.AddressRequestInput
import pl.newbies.generated.inputs.EventRequestInput
import pl.newbies.generated.inputs.TagRequestInput
import pl.newbies.generated.inputs.TimeFrameRequestInput
import pl.newbies.lecture.application.model.LectureRateRequest
import pl.newbies.lecture.application.model.LectureRequest
import pl.newbies.tag.application.model.TagRequest

object TestData {

    val testUser1 = GithubUser(
        id = "1",
        login = "user1",
        name = "user1",
        email = "user1@test.com"
    )

    val testUser2 = GithubUser(
        id = "2",
        login = "user2",
        name = "user2",
        email = "user2@test.com"
    )

    val testUser3 = GithubUser(
        id = "3",
        login = "user3",
        name = "user3",
        email = "user3@test.com"
    )

    val githubUsers = listOf(
        testUser1,
        testUser2,
        testUser3,
    )

    fun createEventRequest(
        title: String = "Event with name",
        subtitle: String? = null,
        timeFrameRequest: TimeFrameRequest = TimeFrameRequest(
            startDate = Instant.fromEpochMilliseconds(1_000L),
            finishDate = Instant.fromEpochMilliseconds(4_000L),
        ),
        address: AddressRequest? = AddressRequest(
            city = "Warszawa",
            place = "Kolońska 45/2",
            coordinates = null,
        ),
        description: String = "Some random description",
        tags: List<TagRequest> = emptyList(),
    ) = EventRequest(
        title = title,
        subtitle = subtitle,
        timeFrame = timeFrameRequest,
        address = address,
        description = description,
        tags = tags,
    )

    fun createEventRequestInput(
        title: String = "Event with name",
        subtitle: String? = null,
        timeFrameRequest: TimeFrameRequestInput = TimeFrameRequestInput(
            startDate = Instant.fromEpochMilliseconds(1_000L).toString(),
            finishDate = Instant.fromEpochMilliseconds(4_000L).toString(),
        ),
        address: AddressRequestInput? = AddressRequestInput(
            city = "Warszawa",
            place = "Kolońska 45/2",
            coordinates = null,
        ),
        description: String = "Some random description",
        tags: List<TagRequestInput> = emptyList(),
    ) = EventRequestInput(
        title = title,
        subtitle = subtitle,
        timeFrame = timeFrameRequest,
        address = address,
        description = description,
        tags = tags,
    )

    fun createLectureRequest(
        title: String = "Lecture name",
        description: String? = null,
        timeFrameRequest: TimeFrameRequest = TimeFrameRequest(
            startDate = Instant.fromEpochMilliseconds(1_000L),
            finishDate = Instant.fromEpochMilliseconds(4_000L),
        ),
        speakerIds: List<String> = listOf()
    ) = LectureRequest(
        title = title,
        description = description,
        timeFrame = timeFrameRequest,
        speakerIds = speakerIds,
    )

    fun createLectureRateRequest(
        topicRate: Int = 3,
        presentationRate: Int = 3,
        opinion: String? = "Some opinion",
    ) = LectureRateRequest(
        topicRate = topicRate,
        presentationRate = presentationRate,
        opinion = opinion,
    )
}