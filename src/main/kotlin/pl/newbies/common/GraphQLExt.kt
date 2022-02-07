package pl.newbies.common

import com.apurebase.kgraphql.Context
import pl.newbies.auth.domain.UnauthorizedException
import pl.newbies.plugins.AresPrincipal

fun Context.principal(): AresPrincipal =
    get() ?: throw UnauthorizedException("No auth principal in context")