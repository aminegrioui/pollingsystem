package org.amine.security.polling.onlinepollingsystem.system.audit.admin.repository

import org.amine.security.polling.onlinepollingsystem.system.audit.admin.model.AdminAuditData
import org.springframework.data.jpa.repository.JpaRepository
import java.time.ZonedDateTime

interface AdminAuditRepository : JpaRepository<AdminAuditData, Long> {
    fun findByTimestampAfterOrTimestamp(timestamp: ZonedDateTime, timestamp2: ZonedDateTime): List<AdminAuditData>
}