package pl.newbies.lecture.application.model

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.extensions.getValueFromDataLoader
import graphql.schema.DataFetchingEnvironment
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import pl.newbies.event.application.model.TimeFrameResponse
import pl.newbies.user.application.model.UserResponse
import java.util.concurrent.CompletableFuture

@Serializable
data class LectureResponse(
    val id: String,
    val title: String,
    val description: String?,
    val timeFrame: TimeFrameResponse,
    val createDate: Instant,
    val updateDate: Instant,
    val author: UserResponse,
    val speakers: List<UserResponse>,
) {
    @GraphQLDescription("Lecture rates summary")
    fun rateSummary(env: DataFetchingEnvironment): CompletableFuture<RateSummary> =
        env.getValueFromDataLoader("LectureRateSummaryDataLoader", id)

    @GraphQLDescription("Lecture rates")
    fun rates(env: DataFetchingEnvironment): CompletableFuture<List<LectureRateResponse>> =
        env.getValueFromDataLoader("LectureRatesDataLoader", id)

    @Serializable
    data class RateSummary(
        val votesCount: Int,
        val topicAvg: Double,
        val presentationAvg: Double,
    )
}