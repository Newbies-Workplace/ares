package pl.newbies.user.application

import pl.newbies.storage.application.FileUrlConverter
import pl.newbies.user.application.model.ContactResponse
import pl.newbies.user.application.model.UserResponse
import pl.newbies.user.domain.model.User

class UserConverter(
    private val fileUrlConverter: FileUrlConverter,
) {

    fun convert(user: User): UserResponse =
        UserResponse(
            id = user.id,
            avatar = user.avatar?.let { avatar -> fileUrlConverter.convert(avatar).url },
            nickname = user.nickname,
            description = user.description,
            contact = ContactResponse(
                github = user.contact.github,
                linkedin = user.contact.linkedin,
                mail = user.contact.mail,
                twitter = user.contact.twitter,
            ),
            createDate = user.createDate,
            updateDate = user.updateDate,
        )
}