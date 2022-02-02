package pl.newbies.common

class DuplicateException(
    override val message: String?
) : Exception(message)