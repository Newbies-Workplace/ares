package pl.newbies.tag.application

import com.apurebase.kgraphql.Context
import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.common.pagination
import pl.newbies.common.principal
import pl.newbies.plugins.AresPrincipal
import pl.newbies.plugins.inject
import pl.newbies.tag.application.model.TagCreateRequest
import pl.newbies.tag.application.model.TagRequest
import pl.newbies.tag.application.model.TagResponse
import pl.newbies.tag.domain.service.TagService
import pl.newbies.tag.infrastructure.repository.*

fun SchemaBuilder.tagSchema() {
    val tagConverter: TagConverter by inject()
    val tagService: TagService by inject()

    query("tags") {
        resolver { page: Int?, size: Int? ->
            val pagination = (page to size).pagination()

            transaction {
                TagDAO.all()
                    .limit(pagination.limit, pagination.offset)
                    .map { it.toTag() }
            }.map { tagConverter.convert(it) }
        }
    }

    query("followedTags") {
        resolver { page: Int?, size: Int?, context: Context ->
            val principal = context.principal()
            val pagination = (page to size).pagination()

            transaction {
                FollowedTagDAO.find { FollowedTags.user eq principal.userId }
                    .limit(pagination.limit, pagination.offset)
                    .map { it.toFollowedTag().tag }
            }.map { tagConverter.convert(it) }
        }
    }

    query("tag") {
        resolver { id: String ->
            transaction {
                TagDAO.findById(id)?.toTag()
            }?.let {
                tagConverter.convert(it)
            }
        }
    }

    mutation("followTags") {
        resolver { request: List<TagRequest>, context: Context ->
            val principal = context.principal()

            val foundTags = transaction {
                TagDAO.find { Tags.id inList request.distinct().map { it.id } }
                    .map { it.toTag() }
            }

            tagService.putFollowedTags(principal.userId, foundTags)
                .map { tagConverter.convert(it.tag) }
        }
    }

    mutation("unfollowTags") {
        resolver { request: List<TagRequest>, context: Context ->
            val principal = context.principal()

            tagService.removeFollowedTags(principal.userId, request.distinct().map { it.id })

            true
        }
    }

    mutation("createTag") {
        resolver { request: TagCreateRequest, context: Context ->
            context.principal()

            val tag = tagService.createTag(request.name)

            tagConverter.convert(tag)
        }
    }

    inputType<TagCreateRequest>()
    inputType<TagRequest>()
    type<TagResponse>()
}