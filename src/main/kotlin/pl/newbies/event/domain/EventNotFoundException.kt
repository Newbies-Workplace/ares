package pl.newbies.event.domain

import pl.newbies.common.NotFoundException

class EventNotFoundException(
    id: String,
) : NotFoundException(id = id, entityName = "Event")