package pl.newbies.common

import io.ktor.server.application.ApplicationCall
import kotlinx.serialization.Serializable
import org.valiktor.functions.isGreaterThanOrEqualTo
import org.valiktor.functions.isLessThanOrEqualTo
import org.valiktor.validate

private const val MIN_PAGE = 1L
private const val MIN_PAGE_SIZE = 1L
private const val MAX_PAGE_SIZE = 50L

@Serializable
data class Pagination(
    val page: Long,
    val size: Long,
) {
    init {
        validate(this) {
            validate(Pagination::page)
                .isGreaterThanOrEqualTo(MIN_PAGE)

            validate(Pagination::size)
                .isGreaterThanOrEqualTo(MIN_PAGE_SIZE)
                .isLessThanOrEqualTo(MAX_PAGE_SIZE)
        }
    }
}

fun ApplicationCall.pagination(
    defaultPage: Long = MIN_PAGE,
    defaultSize: Long = 30L,
): Pagination =
    Pagination(
        page = request.queryParameters["page"]?.toLong() ?: defaultPage,
        size = request.queryParameters["size"]?.toLong() ?: defaultSize,
    )