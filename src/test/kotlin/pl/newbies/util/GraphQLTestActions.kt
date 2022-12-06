package pl.newbies.util

import io.ktor.client.request.bearerAuth
import io.ktor.server.testing.ApplicationTestBuilder
import org.junit.jupiter.api.Assertions.assertTrue
import pl.newbies.auth.application.model.AuthResponse
import pl.newbies.generated.FollowEventMutation
import pl.newbies.generated.RateLectureMutation
import pl.newbies.generated.UnfollowEventMutation
import pl.newbies.generated.inputs.LectureRateRequestInput
import pl.newbies.lecture.application.model.LectureRateRequest

suspend fun ApplicationTestBuilder.rateLecture(
    authResponse: AuthResponse,
    lectureId: String,
    request: LectureRateRequest = TestData.createLectureRateRequest(),
) {
    graphQLClient.execute(
        RateLectureMutation(
            RateLectureMutation.Variables(
                id = lectureId,
                request = LectureRateRequestInput(
                    topicRate = request.topicRate,
                    presentationRate = request.presentationRate,
                    opinion = request.opinion,
                ),
            )
        )
    ) {
        bearerAuth(authResponse.accessToken)
    }
}

suspend fun ApplicationTestBuilder.followEvent(authResponse: AuthResponse, eventId: String) {
    val response = graphQLClient.execute(
        FollowEventMutation(
            FollowEventMutation.Variables(
                id = eventId,
            )
        )
    ) {
        bearerAuth(authResponse.accessToken)
    }

    assertTrue(response.data?.followEvent!!)
}

suspend fun ApplicationTestBuilder.unfollowEvent(authResponse: AuthResponse, eventId: String) {
    val response = graphQLClient.execute(
        UnfollowEventMutation(
            UnfollowEventMutation.Variables(
                id = eventId,
            )
        )
    ) {
        bearerAuth(authResponse.accessToken)
    }

    assertTrue(response.data?.unfollowEvent!!)
}