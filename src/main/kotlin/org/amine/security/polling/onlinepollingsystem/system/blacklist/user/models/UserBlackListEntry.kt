package org.amine.security.polling.onlinepollingsystem.system.blacklist.user.models

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.ZonedDateTime

@Entity
class UserBlackListEntry() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
    var username: String =""
    var token: String =""
    var cause: String =""
    var expireTime: ZonedDateTime? = null
}