package org.amine.security.polling.onlinepollingsystem.system.audit.admin.repository

import org.amine.security.polling.onlinepollingsystem.system.audit.admin.model.ArchiveAdminAuditData
import org.springframework.data.jpa.repository.JpaRepository

interface ArchiveAdminAuditRepository : JpaRepository<ArchiveAdminAuditData, Long> {
}