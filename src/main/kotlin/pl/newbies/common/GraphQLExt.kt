package pl.newbies.common

import graphql.schema.DataFetchingEnvironment
import org.dataloader.BatchLoaderEnvironment
import pl.newbies.auth.domain.UnauthorizedException
import pl.newbies.plugins.AresPrincipal


fun DataFetchingEnvironment.optPrincipal(): AresPrincipal? =
    graphQlContext["PRINCIPAL"]

fun DataFetchingEnvironment.principal(): AresPrincipal =
    optPrincipal() ?: throw UnauthorizedException("No auth principal in context")

fun BatchLoaderEnvironment.principal(): AresPrincipal =
    keyContexts["PRINCIPAL"] as? AresPrincipal ?: throw UnauthorizedException("No auth principal in context")