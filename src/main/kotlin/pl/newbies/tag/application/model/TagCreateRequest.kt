package pl.newbies.tag.application.model

import kotlinx.serialization.Serializable
import org.valiktor.functions.hasSize
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
                .hasSize(min = 2, max = 50)
        }
    }
}