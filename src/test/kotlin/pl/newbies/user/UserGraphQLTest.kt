package pl.newbies.user

import io.ktor.client.request.bearerAuth
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import pl.newbies.common.nanoId
import pl.newbies.generated.ReplaceMyUserMutation
import pl.newbies.generated.UserByIdQuery
import pl.newbies.generated.UserListQuery
import pl.newbies.generated.inputs.ContactRequestInput
import pl.newbies.generated.inputs.UserRequestInput
import pl.newbies.util.*

class UserGraphQLTest : IntegrationTest() {
    @Nested
    inner class UserList {
        @Test
        fun `should return users when called`() = withAres {
            // when
            val response = graphQLClient.execute(
                UserListQuery(
                    UserListQuery.Variables()
                )
            )

            // then
            assertNotNull(response.data?.users)
        }
    }

    @Nested
    inner class UserById {
        @Test
        fun `should return null when called with not existing id`() = withAres {
            // given
            val randomId = nanoId()

            // when
            val response = graphQLClient.execute(
                UserByIdQuery(
                    UserByIdQuery.Variables(
                        id = randomId,
                    )
                )
            )

            // then
            assertNotNull(response.data)
            assertNull(response.data?.user)
        }

        @Test
        fun `should return user when called with valid id`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)

            // when
            val response = graphQLClient.execute(
                UserByIdQuery(
                    UserByIdQuery.Variables(
                        id = authResponse.getUserId(),
                    )
                )
            )

            // then
            assertNotNull(response.data)
            assertNotNull(response.data?.user)
            assertEquals(authResponse.getUserId(), response.data?.user?.id)
        }
    }

    @Nested
    inner class ReplaceMyUser {
        @Test
        fun `should return null when called without authorization`() = withAres {
            // given
            val request = UserRequestInput(contact = ContactRequestInput(), nickname = "Tester testowy")

            // when
            val response = graphQLClient.execute(
                ReplaceMyUserMutation(
                    ReplaceMyUserMutation.Variables(
                        request = request
                    )
                )
            )

            // then
            assertNull(response.data)
            assertNotNull(response.errorAt("replaceMyUser"))
        }

        @Test
        fun `should return updated user when called`() = withAres {
            // given
            val authResponse = loginAs(TestData.testUser1)
            val request = UserRequestInput(contact = ContactRequestInput(), nickname = "Tester testowy")

            // when
            val response = graphQLClient.execute(
                ReplaceMyUserMutation(
                    ReplaceMyUserMutation.Variables(
                        request = request
                    )
                )
            ) {
                bearerAuth(authResponse.accessToken)
            }

            // then
            assertEquals(request.nickname, response.data?.replaceMyUser?.nickname)
        }
    }
}