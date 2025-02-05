package org.amine.security.polling.onlinepollingsystem.system.audit.user.repository

import org.amine.security.polling.onlinepollingsystem.system.audit.admin.model.AdminAuditData
import org.amine.security.polling.onlinepollingsystem.system.audit.user.model.UserAuditData
import org.springframework.data.jpa.repository.JpaRepository
import java.time.ZonedDateTime

interface UserAuditRepository : JpaRepository<UserAuditData, Long> {
    fun findByTimestampAfterOrTimestamp(timestamp: ZonedDateTime, timestamp2: ZonedDateTime): List<AdminAuditData>
}