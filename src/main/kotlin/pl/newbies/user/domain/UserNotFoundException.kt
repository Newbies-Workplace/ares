package pl.newbies.user.domain

import pl.newbies.common.NotFoundException

class UserNotFoundException(
    id: String,
) : NotFoundException(id = id, entityName = "User")