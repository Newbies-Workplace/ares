package pl.newbies.util

import com.expediagroup.graphql.client.types.GraphQLClientError
import com.expediagroup.graphql.client.types.GraphQLClientResponse

fun <T> GraphQLClientResponse<T>.errorAt(path: String): GraphQLClientError? =
    errors?.find { it.path?.contains(path) == true }