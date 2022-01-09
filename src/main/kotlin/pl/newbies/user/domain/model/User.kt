package pl.newbies.user.domain.model

import kotlinx.datetime.Instant
import java.util.*

data class User(
    var id: String = UUID.randomUUID().toString(),

    var nickname: String,

    var accounts: AuthAccountsDTO = AuthAccountsDTO(),

    var contact: ContactDTO = ContactDTO(),

    var description: String? = null
) {
    lateinit var createDate: Instant
}