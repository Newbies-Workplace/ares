package pl.newbies.util

import pl.newbies.auth.application.model.GithubUser

object TestData {

    val testUser1 = GithubUser(
        id = "1",
        login = "user1",
        name = "user1",
        email = "user1@test.com"
    )

    val testUser2 = GithubUser(
        id = "2",
        login = "user2",
        name = "user2",
        email = "user2@test.com"
    )

    val testUser3 = GithubUser(
        id = "3",
        login = "user3",
        name = "user3",
        email = "user3@test.com"
    )

    val githubUsers = listOf(
        testUser1,
        testUser2,
        testUser3,
    )
}