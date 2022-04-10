package pl.newbies.event.application.model

import kotlinx.serialization.Serializable
import pl.newbies.event.domain.model.Event

@Serializable
data class EventVisibilityRequest(
    val visibility: Event.Visibility,
)