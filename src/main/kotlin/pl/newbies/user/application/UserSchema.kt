package pl.newbies.user.application

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import graphql.schema.DataFetchingEnvironment
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.common.pagination
import pl.newbies.common.principal
import pl.newbies.user.application.model.UserRequest
import pl.newbies.user.application.model.UserResponse
import pl.newbies.user.domain.service.UserService
import pl.newbies.user.infrastructure.repository.UserDAO
import pl.newbies.user.infrastructure.repository.toUser

class UserSchema(
    private val userConverter: UserConverter,
    private val userService: UserService,
) {
    inner class Query {
        @GraphQLDescription("Get all users paged")
        fun users(page: Int? = null, size: Int? = null): List<UserResponse> {
            val pagination = (page to size).pagination()

            return transaction {
                UserDAO.all()
                    .limit(pagination.limit, pagination.offset)
                    .map { it.toUser() }
            }.map { userConverter.convert(it) }
        }

        @GraphQLDescription("Get user by id")
        fun user(id: String): UserResponse? {
            return transaction {
                UserDAO.findById(id)?.toUser()
            }?.let {
                userConverter.convert(it)
            }
        }
    }

    inner class Mutation {
        @GraphQLDescription("Replace user data with new data (PUT equivalent)")
        fun replaceMyUser(request: UserRequest, env: DataFetchingEnvironment): UserResponse {
            val principal = env.principal()

            val updatedUser = userService.replaceUser(principal.userId, request)

            return userConverter.convert(updatedUser)
        }
    }
}