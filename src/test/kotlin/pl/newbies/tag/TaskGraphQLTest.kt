package pl.newbies.tag

import io.ktor.client.request.bearerAuth
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import pl.newbies.common.nanoId
import pl.newbies.generated.CreateTagMutation
import pl.newbies.generated.FollowedTagListQuery
import pl.newbies.generated.TagListQuery
import pl.newbies.generated.inputs.TagCreateRequestInput
import pl.newbies.generated.taglistquery.TagResponse
import pl.newbies.tag.application.model.TagCreateRequest
import pl.newbies.util.*

class TaskGraphQLTest : IntegrationTest() {
    @Nested
    inner class CreateTag {
        @ParameterizedTest
        @ValueSource(
            strings = [
                "",
                "a",
            ]
        )
        fun `should return null when called with invalid name`(name: String) = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val request = TagCreateRequestInput(
                name = name,
            )

            // when
            val response = graphQLClient.execute(
                CreateTagMutation(
                    CreateTagMutation.Variables(
                        request = request,
                    )
                )
            ) {
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertNull(response.data)
            assertNotNull(response.errorAt("createTag"))
        }

        @Test
        fun `should return null when user is unauthorized`() = withAres {
            // given
            val request = TagCreateRequestInput(
                name = nanoId(),
            )

            // when
            val response = graphQLClient.execute(
                CreateTagMutation(
                    CreateTagMutation.Variables(
                        request = request,
                    )
                )
            )

            // then
            assertNull(response.data)
            assertNotNull(response.errorAt("createTag"))
        }

        @Test
        fun `should return null called with existing tag name`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val name = "Testy"
            createTag(authResponse, TagCreateRequest(name))
            val request = TagCreateRequestInput(
                name = name,
            )

            // when
            val response = graphQLClient.execute(
                CreateTagMutation(
                    CreateTagMutation.Variables(
                        request = request,
                    )
                )
            )

            // then
            assertNull(response.data)
            assertNotNull(response.errorAt("createTag"))
        }

        @Test
        fun `should create tag when called with valid name`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val request = TagCreateRequestInput(
                name = nanoId(),
            )

            // when
            val response = graphQLClient.execute(
                CreateTagMutation(
                    CreateTagMutation.Variables(
                        request = request,
                    )
                )
            ) {
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertNotNull(response.data?.createTag)
            assertEquals(request.name, response.data?.createTag?.name)
        }
    }

    @Nested
    inner class TagList {
        @Test
        fun `should return empty list when there are no tags`() = withAres {
            // given
            clearTable("Tags")

            // when
            val response = graphQLClient.execute(
                TagListQuery(
                    TagListQuery.Variables()
                )
            )

            // then
            assertEquals(emptyList<TagResponse>(), response.data?.tags)
        }

        @Test
        fun `should return all tags when there are some`() = withAres {
            // given
            clearTable("Tags")
            val authResponse = loginAs(TestData.testUser3)
            val createdTagIds = buildList {
                repeat(2) { add(createTag(authResponse = authResponse).id) }
                add(TagResponse("notExistingId", "notExistingName").id)
            }

            // when
            val response = graphQLClient.execute(
                TagListQuery(
                    TagListQuery.Variables()
                )
            )

            // then
            val responseBody = response.data?.tags!!
            assertEquals(2, responseBody.size)
            assertTrue(
                responseBody.map { it.id }
                    .containsAll(
                        listOf(
                            createdTagIds[0],
                            createdTagIds[1],
                        )
                    )
            )
        }
    }

    @Nested
    inner class FollowTags {
        @Test
        fun `should return null when called without authentication`() = withAres {
            // when
            val response = graphQLClient.execute(
                FollowedTagListQuery(
                    FollowedTagListQuery.Variables()
                )
            )

            // then
            assertNull(response.data)
            assertNotNull(response.errorAt("followedTags"))
        }

        @Test
        fun `should return empty list when no tag is followed`() = withAres {
            // given
            clearTable("Tags")
            val authResponse = loginAs(TestData.testUser3)

            // when
            val response = graphQLClient.execute(
                FollowedTagListQuery(
                    FollowedTagListQuery.Variables()
                )
            ) {
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertEquals(
                emptyList<pl.newbies.generated.followedtaglistquery.TagResponse>(),
                response.data?.followedTags
            )
        }
    }
}