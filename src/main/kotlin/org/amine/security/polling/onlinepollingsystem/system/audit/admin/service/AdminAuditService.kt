package org.amine.security.polling.onlinepollingsystem.system.audit.admin.service

import org.amine.security.polling.onlinepollingsystem.exceptions.ValidationDataException
import org.amine.security.polling.onlinepollingsystem.system.audit.admin.repository.AdminAuditRepository
import org.amine.security.polling.onlinepollingsystem.system.audit.admin.model.ArchiveAdminAuditData
import org.amine.security.polling.onlinepollingsystem.system.audit.admin.repository.ArchiveAdminAuditRepository
import org.amine.security.polling.onlinepollingsystem.system.audit.admin.model.AdminAuditData
import org.amine.security.polling.onlinepollingsystem.system.tools.AppTool
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.util.logging.Logger


@Service
class AdminAuditService(
    private val adminRepository: AdminAuditRepository,
    private val adminAuditRepository: AdminAuditRepository,
    private val appTool: AppTool,
    private val archiveAdminAuditRepository: ArchiveAdminAuditRepository,
) {

    val logger: Logger? = Logger.getLogger(AdminAuditService::class.java.name)

    /**
     *  Kafka
     */
    fun saveAction(
        action: String,
        username: String?,
        clientIpAddress: String,
        reason: String,
        timestamp: ZonedDateTime?,
        tokenExpireTime: ZonedDateTime?,
    ) {
        val auditData = AdminAuditData()
        auditData.action = action
        auditData.username = username!!
        auditData.clientIp = clientIpAddress
        auditData.reason=reason
        auditData.timestamp = timestamp
        auditData.tokenExpireTime = tokenExpireTime
        adminRepository.save(auditData)
    }

    fun findAdminAuditLastTime(numberOfDays: Long): List<AdminAuditData> {
        if (numberOfDays < 1 || numberOfDays > 90) {
            throw ValidationDataException("The number of days must be between 1 and 90.")
        }
        val startOfTodayZonedDateTime = appTool.getNowTime().minusDays(numberOfDays)
        return adminAuditRepository.findByTimestampAfterOrTimestamp(
            startOfTodayZonedDateTime,
            startOfTodayZonedDateTime
        )
    }

    @Scheduled(cron = "0 0 0 */15 * *")
    fun cleanUserAudit() {
        logger!!.info("Start Cleaning User Audit Data")
        val auditDataList = adminAuditRepository.findAll().toMutableList()
        for (auditData in auditDataList) {
            if (appTool.getNowTime().isAfter(auditData.timestamp!!.plusMonths(3))) {
                adminAuditRepository.delete(auditData)
                val archiveAdminAuditData = ArchiveAdminAuditData()
                archiveAdminAuditData.action = auditData.username
                archiveAdminAuditData.username = auditData.username
                archiveAdminAuditData.clientIp = auditData.clientIp
                archiveAdminAuditData.timestamp = auditData.timestamp
                archiveAdminAuditData.tokenExpireTime = auditData.tokenExpireTime
                archiveAdminAuditData.tokenExpireTime = auditData.tokenExpireTime
                archiveAdminAuditData.auditId = auditData.auditId
                archiveAdminAuditRepository.save(archiveAdminAuditData)
            }
        }
        logger!!.info("\n End Cleaning User Audit Data")
    }

}