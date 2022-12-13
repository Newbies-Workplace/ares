package pl.newbies.user.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class User(
    val id: String,
    var avatar: String?,
    var nickname: String,
    var description: String? = null,
    val contact: ContactDTO = ContactDTO(),
    val accounts: AuthAccountsDTO = AuthAccountsDTO(),

    val createDate: Instant = Clock.System.now(),
    var updateDate: Instant = createDate
)