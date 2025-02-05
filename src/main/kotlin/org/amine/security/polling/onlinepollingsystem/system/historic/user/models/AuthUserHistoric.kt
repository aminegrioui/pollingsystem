package org.amine.security.polling.onlinepollingsystem.system.historic.user.models

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.ZonedDateTime

@Entity
class AuthUserHistoric() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
    var username: String = ""
    var operation: String = ""
    var operationTimestamp: ZonedDateTime? = null
    var isSuccessOperation: Boolean = false
}