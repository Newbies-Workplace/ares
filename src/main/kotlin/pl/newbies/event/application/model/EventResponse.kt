package pl.newbies.event.application.model

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.extensions.getValueFromDataLoader
import graphql.schema.DataFetchingEnvironment
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import pl.newbies.tag.application.model.TagResponse
import pl.newbies.user.application.model.UserResponse
import java.util.concurrent.CompletableFuture

@Serializable
data class EventResponse(
    val id: String,
    val authorId: String,
    val title: String,
    val subtitle: String?,
    val timeFrame: TimeFrameResponse,
    val address: AddressResponse?,
    val tags: List<TagResponse>,
    val theme: ThemeResponse,
    val createDate: Instant,
    val updateDate: Instant,
) {

    @GraphQLDescription("Event creator")
    fun author(env: DataFetchingEnvironment): CompletableFuture<UserResponse> =
        env.getValueFromDataLoader("EventAuthorDataLoader", authorId)

    @GraphQLDescription("Is event followed by current user")
    fun isFollowed(env: DataFetchingEnvironment): CompletableFuture<Boolean> =
        env.getValueFromDataLoader("EventIsFollowedDataLoader", id)
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

@Serializable
data class ThemeResponse(
    val primaryColor: String?,
    val secondaryColor: String?,
    val image: String?,
)