package pl.newbies.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.HttpClient
import io.ktor.server.application.Application
import io.ktor.server.auth.Principal
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.jwt
import pl.newbies.auth.application.authenticationRoutes
import pl.newbies.auth.application.devGithubAuthentication
import pl.newbies.auth.application.githubAuthentication
import pl.newbies.auth.application.googleAuthentication

fun Application.configureSecurity(oauthClient: HttpClient) {
    val config = environment.config
    val jwtRealm = config.property("jwt.realm").getString()
    val jwtSecret = config.property("jwt.secret").getString()
    val jwtIssuer = config.property("jwt.issuer").getString()

    authentication {
        jwt("jwt") {
            realm = jwtRealm
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withIssuer(jwtIssuer)
                    .build()
            )
            validate { credential ->
                AresPrincipal(credential.getClaim("id", String::class)!!)
            }
        }
    }

    githubAuthentication(oauthClient)
    devGithubAuthentication(oauthClient)
    googleAuthentication(oauthClient)
    authenticationRoutes()
}

class AresPrincipal(
    val userId: String,
) : Principal