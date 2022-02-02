package pl.newbies.tag.application

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.lecture.infrastructure.repository.Lectures
import pl.newbies.plugins.AresPrincipal
import pl.newbies.plugins.inject
import pl.newbies.tag.application.model.TagCreateRequest
import pl.newbies.tag.application.model.TagRequest
import pl.newbies.tag.domain.service.TagService
import pl.newbies.tag.infrastructure.repository.*

fun Application.tagRoutes() {
    val tagConverter: TagConverter by inject()
    val tagService: TagService by inject()

    routing {
        route("/api/v1/tags") {
            get {
                val foundTags = transaction {
                    TagDAO.all()
                        .orderBy(Tags.name to SortOrder.ASC)
                        .map { it.toTag() }
                }

                val tags = foundTags.map { tagConverter.convert(it) }

                call.respond(tags)
            }

            authenticate("jwt") {
                post {
                    val tagRequest = call.receive<TagCreateRequest>()

                    val createdTag = tagService.createTag(tagRequest.name)

                    call.respond(tagConverter.convert(createdTag))
                }

                get("/@me") {
                    val userId = call.principal<AresPrincipal>()!!.userId

                    val foundTags = transaction {
                        FollowedTagDAO.find { FollowedTags.user eq userId }
                            .map { it.toFollowedTag() }
                    }
                    val followedTags = foundTags.map { tagConverter.convert(it.tag) }

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

                delete("/@me") {
                    val userId = call.principal<AresPrincipal>()!!.userId
                    val tags = call.receive<List<TagRequest>>().distinct()

                    tagService.removeFollowedTags(userId, tags.map { it.id })

                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}