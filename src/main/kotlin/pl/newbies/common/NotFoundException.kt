package pl.newbies.common

open class NotFoundException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(id: String, entityName: String = "Entity") : super("$entityName with id=$id not found.")
}