package pl.newbies.common

open class NotFoundException(
    id: String,
    entityName: String = "Entity"
) : RuntimeException("$entityName with id=$id not found.")