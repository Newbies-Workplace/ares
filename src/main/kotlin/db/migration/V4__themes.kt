package db.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.lecture.infrastructure.repository.Lectures

class V4__themes : BaseJavaMigration() {

    override fun migrate(context: Context?) {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Lectures
            )
        }
    }
}