package pl.newbies.tag.application.model

import kotlinx.serialization.Serializable
import org.valiktor.functions.isNotBlank
import org.valiktor.validate

@Serializable
data class TagCreateRequest(
    val name: String
) {

    init {
        validate(this) {
            validate(TagCreateRequest::name)
                .isNotBlank()
        }
    }
}