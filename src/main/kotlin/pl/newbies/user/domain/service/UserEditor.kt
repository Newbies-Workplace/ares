package pl.newbies.user.domain.service

import io.ktor.server.plugins.BadRequestException
import kotlinx.serialization.json.*
import org.valiktor.functions.isNotBlank
import org.valiktor.validate
import pl.newbies.user.domain.model.User

class UserEditor {

    fun update(obj: User, changes: JsonElement): User {
        var user = obj

        try {
            changes.jsonObject.entries.forEach { (key, value) ->
                user = when (key.lowercase()) {
                    "nickname" -> user.copy(nickname = value.jsonPrimitive.contentOrNull!!)
                    "description" -> user.copy(description = value.jsonPrimitive.contentOrNull)
                    "contact" -> updateContact(user, value)
                    else -> throw BadRequestException("Invalid param ($key)")
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

    private fun updateContact(user: User, value: JsonElement): User {
        var contact = user.contact

        value.jsonObject.entries.forEach { (key, value) ->
            contact = when (key.lowercase()) {
                "github" -> contact.copy(github = value.jsonPrimitive.contentOrNull)
                "linkedin" -> contact.copy(linkedin = value.jsonPrimitive.contentOrNull)
                "mail" -> contact.copy(mail = value.jsonPrimitive.contentOrNull)
                "twitter" -> contact.copy(twitter = value.jsonPrimitive.contentOrNull)
                else -> throw BadRequestException("Invalid param ($key)")
            }
        }

        return user.copy(contact = contact)
    }
}