package pl.newbies.event.application.model

import kotlinx.serialization.Serializable
import org.valiktor.functions.isNotEmpty
import org.valiktor.validate
import pl.newbies.common.validator.distinct
import pl.newbies.event.domain.model.Event

@Serializable
data class EventFilter(
    val authorId: String? = null,
    val visibilityIn: List<Event.Visibility> = listOf(Event.Visibility.PUBLIC)
) {
    init {
        validate(this) {
            validate(EventFilter::visibilityIn)
                .distinct()
                .isNotEmpty()
        }
    }
}
