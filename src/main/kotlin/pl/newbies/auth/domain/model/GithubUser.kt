package pl.newbies.auth.domain.model

data class GithubUser(
    val id: String,
    val login: String,
    var name: String? = null,
    var email: String? = null,
)