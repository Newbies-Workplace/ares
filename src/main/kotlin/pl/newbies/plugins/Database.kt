package pl.newbies.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.log
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager

private fun Application.createHikariDataSource(): HikariDataSource {
    val config = environment.config

    val hikariConfig = HikariConfig().apply {
        driverClassName = config.property("database.driverClassName").getString()
        jdbcUrl = config.property("database.jdbcUrl").getString()
        username = config.property("database.username").getString()
        password = config.property("database.password").getString()
        maximumPoolSize = 3
        isAutoCommit = false
        validate()
    }
    return HikariDataSource(hikariConfig)
}

private fun Application.runMigrations(dataSource: HikariDataSource) {
    val flyway = Flyway.configure()
//        .locations("db/migration")
        .dataSource(dataSource)
        .load()

    try {
        flyway.info()
        flyway.migrate()
    } catch (e: Exception) {
        log.error("Flyway database migration exception", e)

        throw e
    }

    log.info("Flyway database migration finished")
}

open class StringUUIDTable(name: String = "", columnName: String = "id") : IdTable<String>(name) {
    final override val id: Column<EntityID<String>> =
        varchar(columnName, length = 36).entityId()

    final override val primaryKey =
        PrimaryKey(id)
}

abstract class StringUUIDEntity(id: EntityID<String>) : Entity<String>(id)
abstract class StringUUIDEntityClass<out E : StringUUIDEntity>(table: IdTable<String>, entityType: Class<E>? = null) : EntityClass<String, E>(table, entityType)

fun Application.configureDatabase() {
    val dataSource = createHikariDataSource()

    Database.connect(dataSource)
    environment.monitor.subscribe(ApplicationStopping) {
        TransactionManager.currentOrNull()?.close()
        dataSource.close()
    }
    runMigrations(dataSource)
}