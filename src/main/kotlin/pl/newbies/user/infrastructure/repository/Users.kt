package pl.newbies.user.infrastructure.repository

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import pl.newbies.plugins.StringNanoIdEntity
import pl.newbies.plugins.StringNanoIdEntityClass
import pl.newbies.plugins.StringNanoIdTable
import pl.newbies.user.domain.model.AuthAccountsDTO
import pl.newbies.user.domain.model.ContactDTO
import pl.newbies.user.domain.model.User

object Users : StringNanoIdTable() {
    val nickname = varchar("nickname", length = 50, collate = "utf8mb4_unicode_ci")
    val description = varchar("description", length = 255, collate = "utf8mb4_unicode_ci").nullable()

    //accounts
    val githubId = varchar("githubId", length = 30).nullable()
    val devGithubId = varchar("devGithubId", length = 30).nullable()

    //contact
    val github = varchar("contactGithub", length = 50, collate = "utf8_general_ci").nullable()
    val linkedin = varchar("contactLinkedin", length = 50, collate = "utf8_general_ci").nullable()
    val mail = varchar("contactMail", length = 50, collate = "utf8_general_ci").nullable()
    val twitter = varchar("contactTwitter", length = 50, collate = "utf8_general_ci").nullable()

    val createDate = timestamp("createDate")
    val updateDate = timestamp("updateDate")
}

class UserDAO(id: EntityID<String>) : StringNanoIdEntity(id) {
    companion object : StringNanoIdEntityClass<UserDAO>(Users)

    var nickname by Users.nickname
    var description by Users.description

    var githubId by Users.githubId
    var devGithubId by Users.devGithubId

    var github by Users.github
    var linkedin by Users.linkedin
    var mail by Users.mail
    var twitter by Users.twitter

    var createDate by Users.createDate
    var updateDate by Users.updateDate
}

fun UserDAO.toUser() = User(
    id = id.value,
    nickname = nickname,
    accounts = AuthAccountsDTO(
        githubId = githubId,
        devGithubId = devGithubId,
    ),
    contact = ContactDTO(
        github = github,
        linkedin = linkedin,
        mail = mail,
        twitter = twitter,
    ),
    description = description,
    createDate = createDate,
    updateDate = updateDate,
)