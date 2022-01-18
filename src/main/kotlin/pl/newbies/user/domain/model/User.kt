package pl.newbies.user.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.*

data class User(
    val id: String = UUID.randomUUID().toString(),
    var nickname: String,
    var description: String? = null,
    val contact: ContactDTO = ContactDTO(),
    val accounts: AuthAccountsDTO = AuthAccountsDTO(),

    val createDate: Instant = Clock.System.now(),
    var updateDate: Instant = createDate
)