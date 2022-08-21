package pl.newbies.tag.application

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import graphql.schema.DataFetchingEnvironment
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.common.pagination
import pl.newbies.common.principal
import pl.newbies.tag.application.model.TagCreateRequest
import pl.newbies.tag.application.model.TagRequest
import pl.newbies.tag.application.model.TagResponse
import pl.newbies.tag.domain.service.TagService
import pl.newbies.tag.infrastructure.repository.*
import com.expediagroup.graphql.server.operations.Mutation as GraphQLMutation
import com.expediagroup.graphql.server.operations.Query as GraphQLQuery

class TagSchema(
    private val tagConverter: TagConverter,
    private val tagService: TagService,
) {
    inner class Query : GraphQLQuery {
        @GraphQLDescription("Get all tags paged")
        fun tags(page: Int? = null, size: Int? = null): List<TagResponse> {
            val pagination = (page to size).pagination()

            return transaction {
                TagDAO.all()
                    .orderBy(Tags.name to SortOrder.ASC)
                    .limit(pagination.limit, pagination.offset)
                    .map { it.toTag() }
            }.map { tagConverter.convert(it) }
        }

        @GraphQLDescription("Get tags followed by user (paged)")
        fun followedTags(page: Int? = null, size: Int? = null, env: DataFetchingEnvironment): List<TagResponse> {
            val principal = env.principal()
            val pagination = (page to size).pagination()

            return transaction {
                FollowedTagDAO.find { FollowedTags.user eq principal.userId }
                    .limit(pagination.limit, pagination.offset)
                    .map { it.toFollowedTag().tag }
            }.map { tagConverter.convert(it) }
        }

        @GraphQLDescription("Get tag by id")
        fun tag(id: String): TagResponse? {
            return transaction {
                TagDAO.findById(id)?.toTag()
            }?.let {
                tagConverter.convert(it)
            }
        }
    }

    inner class Mutation : GraphQLMutation {
        @GraphQLDescription("Appends new tags to followed list")
        fun followTags(request: List<TagRequest>, env: DataFetchingEnvironment): List<TagResponse> {
            val principal = env.principal()

            val foundTags = transaction {
                TagDAO.find { Tags.id inList request.distinct().map { it.id } }
                    .map { it.toTag() }
            }

            return tagService.putFollowedTags(principal.userId, foundTags)
                .map { tagConverter.convert(it.tag) }
        }

        @GraphQLDescription("Removes tags from followed list")
        fun unfollowTags(request: List<TagRequest>, env: DataFetchingEnvironment): Boolean {
            val principal = env.principal()

            tagService.removeFollowedTags(principal.userId, request.distinct().map { it.id })

            return true
        }

        @GraphQLDescription("Creates tag")
        fun createTag(request: TagCreateRequest, env: DataFetchingEnvironment): TagResponse {
            env.principal()

            val tag = tagService.createTag(request.name)

            return tagConverter.convert(tag)
        }
    }
}