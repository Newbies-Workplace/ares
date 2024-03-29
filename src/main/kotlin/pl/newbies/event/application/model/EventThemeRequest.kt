package pl.newbies.event.application.model

import kotlinx.serialization.Serializable
import org.valiktor.validate
import pl.newbies.common.validator.isHexColor

@Serializable
data class EventThemeRequest(
    val primaryColor: String? = null,
) {

    init {
        validate(this) {
            validate(EventThemeRequest::primaryColor).isHexColor()
        }
    }
}