package pl.newbies.common

import io.ktor.server.application.ApplicationCall
import kotlinx.serialization.Serializable
import org.valiktor.functions.isGreaterThanOrEqualTo
import org.valiktor.functions.isLessThanOrEqualTo
import org.valiktor.validate
import pl.newbies.common.Pagination.Companion.DEFAULT_PAGE_SIZE
import pl.newbies.common.Pagination.Companion.MIN_PAGE

@Serializable
data class Pagination(
    val page: Int,
    val size: Int,
) {
    val limit = size
    val offset = (page - 1L) * size

    init {
        validate(this) {
            validate(Pagination::page)
                .isGreaterThanOrEqualTo(MIN_PAGE)

            validate(Pagination::size)
                .isGreaterThanOrEqualTo(MIN_PAGE_SIZE)
                .isLessThanOrEqualTo(MAX_PAGE_SIZE)
        }
    }

    companion object {
        const val MIN_PAGE = 1
        const val DEFAULT_PAGE_SIZE = 30
        const val MIN_PAGE_SIZE = 1
        const val MAX_PAGE_SIZE = 50
    }
}

fun Pair<Int?, Int?>.pagination() =
    Pagination(
        page = first ?: MIN_PAGE,
        size = second ?: DEFAULT_PAGE_SIZE,
    )

fun ApplicationCall.pagination(
    defaultPage: Int = MIN_PAGE,
    defaultSize: Int = DEFAULT_PAGE_SIZE,
): Pagination =
    Pagination(
        page = request.queryParameters["page"]?.toInt() ?: defaultPage,
        size = request.queryParameters["size"]?.toInt() ?: defaultSize,
    )