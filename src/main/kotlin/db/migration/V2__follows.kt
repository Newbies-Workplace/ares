package db.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.lecture.infrastructure.repository.LectureFollows

class V2__follows : BaseJavaMigration() {

    override fun migrate(context: Context?) {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                LectureFollows
            )
        }
    }
}