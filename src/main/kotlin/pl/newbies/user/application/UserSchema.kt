package pl.newbies.user.application

import com.apurebase.kgraphql.Context
import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.common.pagination
import pl.newbies.common.principal
import pl.newbies.plugins.inject
import pl.newbies.user.application.model.UserRequest
import pl.newbies.user.application.model.UserResponse
import pl.newbies.user.domain.service.UserService
import pl.newbies.user.infrastructure.repository.UserDAO
import pl.newbies.user.infrastructure.repository.toUser

fun SchemaBuilder.userSchema() {
    val userConverter: UserConverter by inject()
    val userService: UserService by inject()

    query("users") {
        resolver { page: Int?, size: Int? ->
            val pagination = (page to size).pagination()

            transaction {
                UserDAO.all()
                    .limit(pagination.limit, pagination.offset)
                    .map { it.toUser() }
            }.map { userConverter.convert(it) }
        }
    }

    query("user") {
        resolver { id: String ->
            transaction {
                UserDAO.findById(id)?.toUser()
            }?.let {
                userConverter.convert(it)
            }
        }
    }

    mutation("replaceMyUser") {
        resolver { request: UserRequest, context: Context ->
            val principal = context.principal()

            val updatedUser = userService.replaceUser(principal.userId, request)

            userConverter.convert(updatedUser)
        }
    }

    inputType<UserRequest>()
    type<UserResponse>()
}