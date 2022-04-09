package pl.newbies.auth.infrastructure.repository

import kotlinx.datetime.Clock
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import pl.newbies.auth.domain.model.RefreshToken
import pl.newbies.plugins.StringNanoIdEntity
import pl.newbies.plugins.StringNanoIdEntityClass
import pl.newbies.plugins.StringNanoIdTable

object RefreshTokens : StringNanoIdTable() {
    val userId = varchar("userId", 36)

    val dateCreated = timestamp("dateCreated")
        .clientDefault { Clock.System.now() }
}

class RefreshTokenDAO(id: EntityID<String>) : StringNanoIdEntity(id) {
    companion object : StringNanoIdEntityClass<RefreshTokenDAO>(RefreshTokens)

    var userId by RefreshTokens.userId
    var dateCreated by RefreshTokens.dateCreated
}

fun RefreshTokenDAO.toRefreshToken() = RefreshToken(
    token = id.value,
    userId = userId,
    dateCreated = dateCreated,
)