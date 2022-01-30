package db.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.auth.infrastructure.repository.RefreshTokens
import pl.newbies.lecture.infrastructure.repository.LectureTags
import pl.newbies.lecture.infrastructure.repository.Lectures
import pl.newbies.tag.infrastructure.repository.FollowedTags
import pl.newbies.tag.infrastructure.repository.Tags
import pl.newbies.user.infrastructure.repository.Users

class V1__init: BaseJavaMigration() {

    override fun migrate(context: Context?) {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Users,
                Tags,
                FollowedTags,
                Lectures,
                LectureTags,
                RefreshTokens,
            )
        }
    }
}