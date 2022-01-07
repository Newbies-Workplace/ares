package pl.newbies.auth.domain

class UnauthorizedException(
    override val message: String?,
) : Exception(message)