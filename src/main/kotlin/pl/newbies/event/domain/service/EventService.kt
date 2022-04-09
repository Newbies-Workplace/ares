package pl.newbies.event.domain.service

import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.event.application.model.EventRequest
import pl.newbies.event.application.model.EventThemeRequest
import pl.newbies.event.domain.model.Event
import pl.newbies.event.domain.model.EventFollow
import pl.newbies.event.infrastructure.repository.*
import pl.newbies.storage.domain.model.EventImageFileResource
import pl.newbies.storage.domain.model.FileResource
import pl.newbies.tag.infrastructure.repository.TagDAO
import pl.newbies.tag.infrastructure.repository.Tags
import pl.newbies.user.domain.model.User
import pl.newbies.user.infrastructure.repository.UserDAO

class EventService {

    fun createEvent(request: EventRequest, authorId: String): Event = transaction {
        val tags = TagDAO.find { Tags.id inList request.tags.map { it.id } }
            .toMutableList()

        val now = Clock.System.now()

        EventDAO.new {
            this.title = request.title
            this.subtitle = request.subtitle
            this.author = UserDAO[authorId]
            request.timeFrame.let { frame ->
                this.startDate = frame.startDate
                this.finishDate = frame.finishDate
            }
            request.address?.let { address ->
                this.city = address.city
                this.place = address.place
                address.coordinates?.let { coordinates ->
                    this.latitude = coordinates.latitude
                    this.longitude = coordinates.longitude
                }
            }
            this.tags = SizedCollection(tags)

            this.createDate = now
            this.updateDate = now
        }.toEvent()
    }

    fun updateEvent(event: Event, request: EventRequest): Event = transaction {
        val tags = TagDAO.find { Tags.id inList request.tags.map { it.id } }
            .toMutableList()

        EventDAO[event.id]
            .apply {
                this.title = request.title
                this.subtitle = request.subtitle
                request.timeFrame.let { frame ->
                    this.startDate = frame.startDate
                    this.finishDate = frame.finishDate
                }
                request.address.let { address ->
                    this.city = address?.city
                    this.place = address?.place
                    address?.coordinates.let { coordinates ->
                        this.latitude = coordinates?.latitude
                        this.longitude = coordinates?.longitude
                    }
                }
                this.tags = SizedCollection(tags)

                this.updateDate = Clock.System.now()
            }
            .toEvent()
    }

    fun updateTheme(event: Event, request: EventThemeRequest): Event = transaction {
        EventDAO[event.id]
            .apply {
                this.primaryColor = request.primaryColor
                this.secondaryColor = request.secondaryColor
            }
            .toEvent()
    }

    fun updateThemeImage(event: Event, fileResource: FileResource?): Event = transaction {
        EventDAO[event.id]
            .apply {
                this.image = fileResource?.pathWithName
            }
            .toEvent()
    }

    fun deleteEvent(event: Event) = transaction {
        EventDAO[event.id].delete()
    }

    fun followEvent(user: User, event: Event): EventFollow =
        transaction {
            EventFollowDAO.find { (EventFollows.user eq user.id) and (EventFollows.event eq event.id) }
                .firstOrNull()
                ?.toEventFollow()
                ?.let { return@transaction it }

            EventFollowDAO.new {
                this.user = UserDAO[user.id]
                this.event = EventDAO[event.id]

                this.followDate = Clock.System.now()
            }.toEventFollow()
        }

    fun unfollowEvent(user: User, event: Event) {
        transaction {
            EventFollowDAO.find { (EventFollows.user eq user.id) and (EventFollows.event eq event.id) }
                .firstOrNull()
                ?.delete()
        }
    }

    fun getThemeImageFileResource(event: Event): EventImageFileResource? {
        val nameWithExtension = event.theme.image?.substringAfterLast('/')
            ?: return null

        return EventImageFileResource(
            eventId = event.id,
            nameWithExtension = nameWithExtension,
        )
    }
}