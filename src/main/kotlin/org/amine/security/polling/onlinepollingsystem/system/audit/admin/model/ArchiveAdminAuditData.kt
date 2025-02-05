package org.amine.security.polling.onlinepollingsystem.system.audit.admin.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.ZonedDateTime

@Entity
class ArchiveAdminAuditData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0
    var auditId: Long = 0
    var action: String = ""
    var username: String = ""
    var clientIp: String = ""
    var timestamp: ZonedDateTime? = null
    var tokenExpireTime: ZonedDateTime? = null
}