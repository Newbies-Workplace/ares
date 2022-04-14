package pl.newbies.event.domain.service

import kotlinx.datetime.Clock
import org.apache.commons.lang3.StringUtils
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.common.Pagination
import pl.newbies.common.nanoId
import pl.newbies.event.application.model.EventFilter
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
            appendRequestFields(request)
            this.vanityUrl = getEventVanityUrl(title)
            this.author = UserDAO[authorId]
            this.tags = SizedCollection(tags)
            this.visibility = Event.Visibility.PRIVATE
            this.createDate = now
            this.updateDate = now
        }.toEvent()
    }

    fun getEvents(pagination: Pagination, filter: EventFilter, requesterId: String?): List<Event> = transaction {
        var query: Op<Boolean> = getListVisibilityQuery(filter.visibilityIn, requesterId)

        filter.authorId?.run { query = query and (Events.author eq filter.authorId) }

        EventDAO.find { query }
            .orderBy(Events.createDate to SortOrder.ASC)
            .limit(pagination.limit, pagination.offset)
            .map { it.toEvent() }
    }

    fun updateEvent(event: Event, request: EventRequest): Event = transaction {
        val tags = TagDAO.find { Tags.id inList request.tags.map { it.id } }
            .toMutableList()

        EventDAO[event.id]
            .apply {
                appendRequestFields(request)
                this.tags = SizedCollection(tags)

                this.updateDate = Clock.System.now()
            }
            .toEvent()
    }

    fun updateVisibility(event: Event, visibility: Event.Visibility) = transaction {
        EventDAO[event.id]
            .apply {
                this.visibility = visibility
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

    private fun EventDAO.appendRequestFields(request: EventRequest) {
        val title = request.title.trim()

        this.title = title
        this.subtitle = request.subtitle?.trim()
        request.timeFrame.let { frame ->
            this.startDate = frame.startDate
            this.finishDate = frame.finishDate
        }
        request.address.let { address ->
            this.city = address?.city?.trim()
            this.place = address?.place?.trim()
            address?.coordinates.let { coordinates ->
                this.latitude = coordinates?.latitude
                this.longitude = coordinates?.longitude
            }
        }
    }

    private fun getEventVanityUrl(title: String): String {
        var normalized = StringUtils.stripAccents(title)
            .trim()
            .filter { it.isLetterOrDigit() || it in listOf(' ') }
            .replace(Regex("[ \\t]{2,}"), " ") // multiple spaces to one space
            .replace(' ', '-')
            .lowercase()
            .take(44)

        if (normalized.length < VANITY_URL_MIN_SIZE) {
            normalized += nanoId().take(VANITY_URL_MIN_SIZE - normalized.length)
        }

        if (EventDAO.count(Events.vanityUrl eq normalized) > 0) {
            normalized += ("-" + nanoId().take(5))
        }

        return normalized
    }

    private fun getListVisibilityQuery(visibilityIn: List<Event.Visibility>, requesterId: String?): Op<Boolean> {
        var query: Op<Boolean> = Op.FALSE

        if (visibilityIn.contains(Event.Visibility.PUBLIC)) {
            query = query or (Events.visibility eq Event.Visibility.PUBLIC)
        }
        if (requesterId != null) {
            if (visibilityIn.contains(Event.Visibility.INVISIBLE)) {
                query = query or ((Events.visibility eq Event.Visibility.INVISIBLE) and (Events.author eq requesterId))
            }
            if (visibilityIn.contains(Event.Visibility.PRIVATE)) {
                query = query or ((Events.visibility eq Event.Visibility.PRIVATE) and (Events.author eq requesterId))
            }
        }

        return query
    }

    companion object {
        const val VANITY_URL_MIN_SIZE = 10
    }
}