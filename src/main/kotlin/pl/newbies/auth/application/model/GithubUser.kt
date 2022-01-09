package pl.newbies.auth.application.model

import kotlinx.serialization.Serializable

@Serializable
data class GithubUser(
    val id: String,
    val login: String,
    var name: String? = null,
    var email: String? = null,
)