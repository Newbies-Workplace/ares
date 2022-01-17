package pl.newbies.user.domain.service

import io.ktor.server.plugins.BadRequestException
import org.valiktor.functions.isNotBlank
import org.valiktor.validate
import pl.newbies.user.domain.model.User

class UserEditor {

    fun update(obj: User, changes: Map<String, String?>): User {
        var user = obj

        try {
            changes.forEach { (key, value) ->
                user = when (key.lowercase()) {
                    "nickname" -> user.copy(nickname = value!!)
                    "description" -> user.copy(description = value)
                    "linkedin" -> user.copy(contact = user.contact.copy(linkedin = value))
                    "github" -> user.copy(contact = user.contact.copy(github = value))
                    "mail" -> user.copy(contact = user.contact.copy(mail = value))
                    "twitter" -> user.copy(contact = user.contact.copy(twitter = value))
                    else -> user
                }
            }

            validate(user) {
                validate(User::nickname).isNotBlank()
            }
        } catch (e: Exception) {
            throw BadRequestException(e.message.orEmpty(), e)
        }

        return user
    }
}
