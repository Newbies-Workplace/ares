package pl.newbies.common

class ForbiddenException(
    userId: String,
    entityId: String,
) : RuntimeException("User ($userId) is not authorized to access entity ($entityId)")