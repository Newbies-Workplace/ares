package pl.newbies.auth.infrastructure.repository

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import pl.newbies.auth.domain.model.RefreshToken
import pl.newbies.plugins.StringNanoIdEntity
import pl.newbies.plugins.StringNanoIdEntityClass
import pl.newbies.plugins.StringNanoIdTable

object RefreshTokens : StringNanoIdTable() {
    val family = varchar("family", 36)
    val userId = varchar("userId", 36)

    val isUsed = bool("isUsed")

    val dateExpired = timestamp("dateExpired")
    val dateCreated = timestamp("dateCreated")
}

class RefreshTokenDAO(id: EntityID<String>) : StringNanoIdEntity(id) {
    companion object : StringNanoIdEntityClass<RefreshTokenDAO>(RefreshTokens)

    var family by RefreshTokens.family
    var userId by RefreshTokens.userId

    var isUsed by RefreshTokens.isUsed

    var dateExpired by RefreshTokens.dateExpired
    var dateCreated by RefreshTokens.dateCreated
}

fun RefreshTokenDAO.toRefreshToken() = RefreshToken(
    token = id.value,
    family = family,
    userId = userId,
    isUsed = isUsed,
    dateExpired = dateExpired,
    dateCreated = dateCreated,
)