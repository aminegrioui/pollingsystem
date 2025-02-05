package org.amine.security.polling.onlinepollingsystem.system.security.jwt

import org.springframework.security.core.GrantedAuthority
import java.util.*

class ClaimsResponse {
    var id: Long = 0
    var expireTimeOfRefreshToken: Date? = null
    var expireTimeOfAccessToken: Date? = null
    var issueDateOfToken: Date? = null
    var isAdmin: Boolean = false
    var username: String? =""
    var permissions: Collection<GrantedAuthority?>? = null
}