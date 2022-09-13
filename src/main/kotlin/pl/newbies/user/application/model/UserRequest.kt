package pl.newbies.user.application.model

import kotlinx.serialization.Serializable
import org.valiktor.functions.isNotBlank
import org.valiktor.validate

@Serializable
class UserRequest(
    val nickname: String,
    val description: String? = null,
    val contact: ContactRequest,
) {

    init {
        validate(this) {
            validate(UserRequest::nickname).isNotBlank()
        }
    }
}

@Serializable
data class ContactRequest(
    val github: String? = null,
    val linkedin: String? = null,
    val mail: String? = null,
    val twitter: String? = null,
)