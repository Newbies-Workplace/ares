package pl.newbies.user.domain.service

import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonElement
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.common.logger
import pl.newbies.storage.domain.model.FileResource
import pl.newbies.storage.domain.model.UserAvatarImageFileResource
import pl.newbies.user.application.model.UserRequest
import pl.newbies.user.domain.UserNotFoundException
import pl.newbies.user.domain.model.User
import pl.newbies.user.infrastructure.repository.UserDAO
import pl.newbies.user.infrastructure.repository.toUser

class UserService(
    private val userEditor: UserEditor,
) {

    private val logger = logger()

    fun createUser(
        nickname: String,
        githubId: String? = null,
        devgithubId: String? = null,
    ): User = transaction {
        logger.info("Creating user (githubId = $githubId, devgithubId = $devgithubId)")

        val now = Clock.System.now()

        UserDAO.new {
            this.nickname = nickname

            this.githubId = githubId
            this.devGithubId = devgithubId

            this.createDate = now
            this.updateDate = now
        }.toUser()
    }

    fun updateUser(
        user: User,
        changes: JsonElement,
    ): User = transaction {
        logger.info("Updating user (id = ${user.id})")

        val updatedUser = userEditor.update(user, changes)

        UserDAO.findById(user.id)
            ?.apply {
                nickname = updatedUser.nickname
                description = updatedUser.description
                linkedin = updatedUser.contact.linkedin
                mail = updatedUser.contact.mail
                github = updatedUser.contact.github
                twitter = updatedUser.contact.twitter

                updateDate = Clock.System.now()
            }
            ?.toUser()
            ?: throw UserNotFoundException(user.id)
    }

    fun replaceUser(
        userId: String,
        userRequest: UserRequest,
    ): User = transaction {
        logger.info("Replacing user (id = $userId)")

        UserDAO.findById(userId)
            ?.apply {
                nickname = userRequest.nickname
                description = userRequest.description
                github = userRequest.contact.github
                linkedin = userRequest.contact.linkedin
                mail = userRequest.contact.mail
                twitter = userRequest.contact.twitter

                updateDate = Clock.System.now()
            }
            ?.toUser()
            ?: throw UserNotFoundException(userId)
    }

    fun getAvatarFileResource(user: User): UserAvatarImageFileResource? {
        val nameWithExtension = user.avatar?.substringAfterLast('/')
            ?: return null

        return UserAvatarImageFileResource(
            userId = user.id,
            nameWithExtension = nameWithExtension,
        )
    }

    fun updateUserAvatar(user: User, fileResource: FileResource?): User = transaction {
        UserDAO[user.id]
            .apply {
                this.avatar = fileResource?.pathWithName

                this.updateDate = Clock.System.now()
            }
            .toUser()
    }
}