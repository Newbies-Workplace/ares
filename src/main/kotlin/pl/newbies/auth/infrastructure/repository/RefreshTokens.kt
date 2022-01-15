package pl.newbies.auth.infrastructure.repository

import kotlinx.datetime.Clock
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import pl.newbies.auth.domain.model.RefreshToken
import pl.newbies.plugins.StringUUIDEntity
import pl.newbies.plugins.StringUUIDEntityClass
import pl.newbies.plugins.StringUUIDTable

object RefreshTokens : StringUUIDTable() {
    val userId = varchar("userId", 36)

    val dateCreated = timestamp("dateCreated")
        .clientDefault { Clock.System.now() }
}

class RefreshTokenDAO(id: EntityID<String>) : StringUUIDEntity(id) {
    companion object : StringUUIDEntityClass<RefreshTokenDAO>(RefreshTokens)

    var userId by RefreshTokens.userId
    var dateCreated by RefreshTokens.dateCreated
}

fun RefreshTokenDAO.toRefreshToken() = RefreshToken(
    token = id.value,
    userId = userId,
    dateCreated = dateCreated,
)