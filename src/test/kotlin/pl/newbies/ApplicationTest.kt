package pl.newbies

import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test

val defaultConfig = MapApplicationConfig(
    "database.driverClassName" to "org.h2.Driver",
    "database.jdbcUrl" to "jdbc:h2:mem:test",
    "jwt.realm" to "Test Realm",
    "jwt.secret" to "secret",
    "jwt.issuer" to "Test issuer",
    "oauth.github.userUrl" to "",
    "database.username" to "root",
    "database.password" to "",
)

class ApplicationTest {

    @Test
    fun testRoot() {
        testApplication {
            application {
                testModule()
                module()
            }
            environment { config = defaultConfig }
        }
    }
}