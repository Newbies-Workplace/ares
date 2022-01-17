package pl.newbies.user.domain.service

import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.auth.infrastructure.repository.RefreshTokens.userId
import pl.newbies.common.logger
import pl.newbies.user.application.model.UserRequest
import pl.newbies.user.domain.UserNotFoundException
import pl.newbies.user.domain.model.User
import pl.newbies.user.infrastructure.repository.UserDAO
import pl.newbies.user.infrastructure.repository.toUser
import java.util.*

class UserService(
    private val userEditor: UserEditor,
) {

    private val logger = logger()

    fun createUser(
        nickname: String,
        githubId: String,
    ): User = transaction {
        logger.info("Creating user (githubId = $githubId)")

        UserDAO.new(UUID.randomUUID().toString()) {
            this.nickname = nickname
            this.githubId = githubId
        }.toUser()
    }

    fun updateUser(
        user: User,
        changes: Map<String, String?>,
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
            }
            ?.toUser()
            ?: throw UserNotFoundException(userId)
    }
}