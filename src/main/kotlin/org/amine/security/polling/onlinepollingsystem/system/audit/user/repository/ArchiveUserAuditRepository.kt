package org.amine.security.polling.onlinepollingsystem.system.audit.user.repository

import org.amine.security.polling.onlinepollingsystem.system.audit.user.model.ArchiveUserAuditData
import org.springframework.data.jpa.repository.JpaRepository

interface ArchiveUserAuditRepository : JpaRepository<ArchiveUserAuditData, Long> {
}