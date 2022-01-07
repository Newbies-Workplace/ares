package pl.newbies.user.infrastructure.repository

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.javatime.timestamp
import pl.newbies.plugins.StringUUIDEntity
import pl.newbies.plugins.StringUUIDEntityClass
import pl.newbies.plugins.StringUUIDTable
import pl.newbies.user.domain.model.AuthAccountsDTO
import pl.newbies.user.domain.model.ContactDTO
import pl.newbies.user.domain.model.User
import java.time.Instant

object Users : StringUUIDTable() {
    val nickname = varchar("nickname", length = 50)
    val description = varchar("description", length = 255).nullable()

    //accounts
    val githubId = varchar("githubId", length = 30).nullable()

    //contact
    val github = varchar("contactGithub", length = 50).nullable()
    val linkedin = varchar("contactLinkedin", length = 50).nullable()
    val mail = varchar("contactMail", length = 50).nullable()
    val twitter = varchar("contactTwitter", length = 50).nullable()

    val createDate = timestamp("createDate")
        .clientDefault { Instant.now() }
}

class UserDAO(id: EntityID<String>) : StringUUIDEntity(id) {
    companion object : StringUUIDEntityClass<UserDAO>(Users)

    var nickname by Users.nickname
    var description by Users.description

    var githubId by Users.githubId

    var github by Users.github
    var linkedin by Users.linkedin
    var mail by Users.mail
    var twitter by Users.twitter

    var createDate by Users.createDate
}

fun UserDAO.toUser() = User(
    id = id.value,
    nickname = nickname,
    accounts = AuthAccountsDTO(
        githubId = githubId,
    ),
    contact = ContactDTO(
        github = github,
        linkedin = linkedin,
        mail = mail,
        twitter = twitter,
    ),
    description = description,
).apply {
    createDate = this@toUser.createDate
}