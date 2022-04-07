package pl.newbies.event.application

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.execution.KotlinDataLoader
import graphql.schema.DataFetchingEnvironment
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.common.pagination
import pl.newbies.common.principal
import pl.newbies.event.application.model.EventRequest
import pl.newbies.event.application.model.EventResponse
import pl.newbies.event.application.model.EventThemeRequest
import pl.newbies.event.domain.EventNotFoundException
import pl.newbies.event.domain.service.EventService
import pl.newbies.event.infrastructure.repository.EventDAO
import pl.newbies.event.infrastructure.repository.EventFollows
import pl.newbies.event.infrastructure.repository.Events
import pl.newbies.event.infrastructure.repository.toEvent
import pl.newbies.user.application.UserConverter
import pl.newbies.user.application.model.UserResponse
import pl.newbies.user.domain.UserNotFoundException
import pl.newbies.user.infrastructure.repository.UserDAO
import pl.newbies.user.infrastructure.repository.toUser
import java.util.concurrent.CompletableFuture

class EventSchema(
    private val eventConverter: EventConverter,
    private val eventService: EventService,
    private val userConverter: UserConverter,
) {
    inner class Query {
        @GraphQLDescription("Get all events paged")
        fun events(page: Int? = null, size: Int? = null): List<EventResponse> {
            val pagination = (page to size).pagination()

            return transaction {
                EventDAO.all()
                    .orderBy(Events.createDate to SortOrder.ASC)
                    .limit(pagination.limit, pagination.offset)
                    .map { it.toEvent() }
            }.map { eventConverter.convert(it) }
        }

        @GraphQLDescription("Get single event by its id")
        fun event(id: String): EventResponse? {
            return transaction {
                EventDAO.findById(id)?.toEvent()
            }?.let {
                eventConverter.convert(it)
            }
        }
    }

    inner class Mutation {
        @GraphQLDescription("Create event with request")
        fun createEvent(request: EventRequest, env: DataFetchingEnvironment): EventResponse {
            val principal = env.principal()

            val event = eventService.createEvent(request, principal.userId)

            return eventConverter.convert(event)
        }

        @GraphQLDescription("Replace event data with new data (PUT equivalent)")
        fun replaceEvent(id: String, request: EventRequest, env: DataFetchingEnvironment): EventResponse {
            val principal = env.principal()
            val event = transaction { EventDAO.findById(id)?.toEvent() }
                ?: throw EventNotFoundException(id)

            principal.assertEventWriteAccess(event)

            val replacedEvent = eventService.updateEvent(event, request)

            return eventConverter.convert(replacedEvent)
        }

        @GraphQLDescription("Replace event theme with new one (PUT equivalent)")
        fun replaceEventTheme(
            id: String,
            request: EventThemeRequest,
            env: DataFetchingEnvironment
        ): EventResponse {
            val principal = env.principal()
            val event = transaction { EventDAO.findById(id)?.toEvent() }
                ?: throw EventNotFoundException(id)

            principal.assertEventWriteAccess(event)

            val updatedEvent = eventService.updateTheme(event, request)

            return eventConverter.convert(updatedEvent)
        }

        @GraphQLDescription("Delete event by id")
        fun deleteEvent(id: String, env: DataFetchingEnvironment): Boolean {
            val principal = env.principal()
            val event = transaction { EventDAO.findById(id)?.toEvent() }
                ?: throw EventNotFoundException(id)

            principal.assertEventWriteAccess(event)

            eventService.deleteEvent(event)

            return true
        }

        @GraphQLDescription("Follow event by id")
        fun followEvent(id: String, env: DataFetchingEnvironment): Boolean {
            val principal = env.principal()

            val user = transaction { UserDAO.findById(principal.userId)?.toUser() }
                ?: throw UserNotFoundException(principal.userId)
            val event = transaction { EventDAO.findById(id)?.toEvent() }
                ?: throw EventNotFoundException(id)

            eventService.followEvent(user, event)

            return true
        }

        @GraphQLDescription("Unfollow event by id")
        fun unfollowEvent(id: String, env: DataFetchingEnvironment): Boolean {
            val principal = env.principal()

            val user = transaction { UserDAO.findById(principal.userId)?.toUser() }
                ?: throw UserNotFoundException(principal.userId)
            val event = transaction { EventDAO.findById(id)?.toEvent() }
                ?: throw EventNotFoundException(id)

            eventService.unfollowEvent(user, event)

            return true
        }
    }

    inner class AuthorDataLoader : KotlinDataLoader<String, UserResponse> {
        override val dataLoaderName: String = "EventAuthorDataLoader"

        override fun getDataLoader(): DataLoader<String, UserResponse> =
            DataLoaderFactory.newDataLoader { authorIds ->
                CompletableFuture.supplyAsync {
                    val users = transaction {
                        UserDAO.forIds(authorIds).map { it.toUser() }
                    }

                    users.map { userConverter.convert(it) }
                }
            }
    }

    inner class IsFollowedDataLoader : KotlinDataLoader<String, Boolean> {
        override val dataLoaderName: String = "EventIsFollowedDataLoader"

        override fun getDataLoader(): DataLoader<String, Boolean> =
            DataLoaderFactory.newDataLoader { eventIds, env ->
                CompletableFuture.supplyAsync {
                    val principal = env.principal()
                    val followedMap = mutableMapOf<String, Boolean>()

                    transaction {
                        eventIds.forEach { eventId ->
                            val follow = EventDAO.find {
                                (EventFollows.user eq principal.userId) and (EventFollows.event eq eventId)
                            }.firstOrNull()

                            followedMap[eventId] = follow != null
                        }
                    }

                    followedMap.values.toList()
                }
            }
    }
}