package pl.newbies.tag.application

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.common.pagination
import pl.newbies.plugins.AresPrincipal
import pl.newbies.plugins.inject
import pl.newbies.tag.application.model.TagRequest
import pl.newbies.tag.domain.service.TagService
import pl.newbies.tag.infrastructure.repository.*

fun Application.tagRoutes() {
    val tagConverter: TagConverter by inject()
    val tagService: TagService by inject()

    routing {
        route("/api/v1/tags") {
            get {
                val (page, size) = call.pagination()

                val foundTags = transaction {
                    TagDAO.all().limit(page.toInt(), page * size)
                }

                val tags = foundTags.map { tagConverter.convert(it.toTag()) }

                call.respond(tags)
            }

            get("/@me") {
                val userId = call.principal<AresPrincipal>()!!.userId

                val foundTags = transaction {
                    FollowedTagDAO.find { FollowedTags.user eq userId }
                }
                val followedTags = foundTags.map { tagConverter.convert(it.toFollowedTag().tag) }

                call.respond(followedTags)
            }

            put("/@me") {
                val userId = call.principal<AresPrincipal>()!!.userId
                val tags = call.receive<List<TagRequest>>().distinct()
                val foundTags = transaction {
                    TagDAO.find { Tags.id inList tags.map { it.id } }
                        .map { it.toTag() }
                }

                val followedTags = tagService.putFollowedTags(userId, foundTags)
                    .map { tagConverter.convert(it.tag) }

                call.respond(followedTags)
            }
        }
    }
}