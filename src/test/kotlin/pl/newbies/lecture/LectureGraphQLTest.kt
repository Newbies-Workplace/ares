package pl.newbies.lecture

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import pl.newbies.generated.LectureByEventIdQuery
import pl.newbies.generated.LectureByIdQuery
import pl.newbies.generated.inputs.LectureFilterInput
import pl.newbies.lecture.application.model.LectureRateRequest
import pl.newbies.util.*

class LectureGraphQLTest : IntegrationTest() {

    @Nested
    inner class RateLecture {

        @Test
        fun `should rate lecture when called`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse)
            val lecture = createLecture(authResponse, event.id)

            // when
            rateLecture(authResponse, lectureId = lecture.id)

            // then
            val response = graphQLClient.execute(
                LectureByIdQuery(
                    LectureByIdQuery.Variables(
                        id = lecture.id,
                    )
                )
            )

            val data = response.data?.lecture!!
            assertEquals(1, data.rates.size)
        }

        @Test
        fun `should rate lecture when called multiple times`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse)
            val lecture = createLecture(authResponse, event.id)

            // when
            rateLecture(authResponse, lectureId = lecture.id)
            rateLecture(authResponse, lectureId = lecture.id)
            rateLecture(authResponse, lectureId = lecture.id)

            // then
            val response = graphQLClient.execute(
                LectureByIdQuery(
                    LectureByIdQuery.Variables(
                        id = lecture.id,
                    )
                )
            )

            val data = response.data?.lecture!!
            assertEquals(3, data.rates.size)
        }

        @Test
        fun `should rate multiple lectures when called`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse)
            val lecture = createLecture(authResponse, event.id)

            // when
            rateLecture(authResponse, lectureId = lecture.id)
            rateLecture(authResponse, lectureId = lecture.id)
            rateLecture(authResponse, lectureId = lecture.id)

            // then
            val response = graphQLClient.execute(
                LectureByIdQuery(
                    LectureByIdQuery.Variables(
                        id = lecture.id,
                    )
                )
            )

            val data = response.data?.lecture!!
            assertEquals(3, data.rates.size)
        }

        @Test
        fun `should rate multiple lecture`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse)
            val firstLecture = createLecture(authResponse, event.id)
            val secondLecture = createLecture(authResponse, event.id)
            val thirdLecture = createLecture(authResponse, event.id)

            // when
            rateLecture(authResponse, lectureId = firstLecture.id)
            rateLecture(authResponse, lectureId = secondLecture.id)

            // then
            val response = graphQLClient.execute(
                LectureByEventIdQuery(
                    variables = LectureByEventIdQuery.Variables(
                        filter = LectureFilterInput(eventId = event.id)
                    )
                )
            )

            val data = response.data?.lectures!!
            assertEquals(2, data.map { it.rates }.flatten().size)
        }
    }

    @Nested
    inner class LectureRateSummary {

        @Test
        fun `should return rate summary when there is no rates`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse)
            val lecture = createLecture(authResponse, event.id)

            // then
            val response = graphQLClient.execute(
                LectureByIdQuery(
                    LectureByIdQuery.Variables(
                        id = lecture.id,
                    )
                )
            )

            val data = response.data?.lecture!!
            assertAll(
                { assertEquals(0, data.rates.size) },
                { assertEquals(0, data.rateSummary.votesCount) },
                { assertEquals(0.0, data.rateSummary.topicAvg) },
                { assertEquals(0.0, data.rateSummary.presentationAvg) },
            )
        }

        @Test
        fun `should return rate summary when queried`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val event = createEvent(authResponse)
            val lecture = createLecture(authResponse, event.id)

            // when
            rateLecture(
                authResponse = authResponse,
                lectureId = lecture.id,
                request = LectureRateRequest(
                    presentationRate = 4,
                    topicRate = 3,
                    opinion = "Test",
                )
            )
            rateLecture(
                authResponse = authResponse,
                lectureId = lecture.id,
                request = LectureRateRequest(
                    presentationRate = 3,
                    topicRate = 4,
                    opinion = null,
                )
            )

            // then
            val response = graphQLClient.execute(
                LectureByIdQuery(
                    LectureByIdQuery.Variables(
                        id = lecture.id,
                    )
                )
            )

            val data = response.data?.lecture!!
            assertAll(
                { assertEquals(3, data.rates.size) },
                { assertEquals(2, data.rateSummary.votesCount) },
                { assertEquals(3.5, data.rateSummary.topicAvg) },
                { assertEquals(3.5, data.rateSummary.presentationAvg) },
            )
        }
    }
}