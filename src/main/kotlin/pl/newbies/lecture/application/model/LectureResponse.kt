package pl.newbies.lecture.application.model

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.extensions.getValueFromDataLoader
import graphql.schema.DataFetchingEnvironment
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import pl.newbies.tag.application.model.TagResponse
import pl.newbies.user.application.model.UserResponse
import java.util.concurrent.CompletableFuture

@Serializable
data class LectureResponse(
    val id: String,
    val authorId: String,
    val title: String,
    val subtitle: String?,
    val timeFrame: TimeFrameResponse,
    val address: AddressResponse?,
    val tags: List<TagResponse>,
    val createDate: Instant,
    val updateDate: Instant,
) {

    @GraphQLDescription("Lecture creator")
    fun author(env: DataFetchingEnvironment): CompletableFuture<UserResponse> =
        env.getValueFromDataLoader("LectureAuthorDataLoader", authorId)

    @GraphQLDescription("Is lecture followed by current user")
    fun isFollowed(env: DataFetchingEnvironment): CompletableFuture<Boolean> =
        env.getValueFromDataLoader("LectureIsFollowedDataLoader", id)
}

@Serializable
data class TimeFrameResponse(
    val startDate: Instant,
    val finishDate: Instant?,
)

@Serializable
data class AddressResponse(
    val city: String,
    val place: String,
    val coordinates: CoordinatesResponse?,
)

@Serializable
data class CoordinatesResponse(
    val latitude: Double,
    val longitude: Double,
)