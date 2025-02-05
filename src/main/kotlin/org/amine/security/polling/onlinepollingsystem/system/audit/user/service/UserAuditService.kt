package org.amine.security.polling.onlinepollingsystem.system.audit.user.service

import org.amine.security.polling.onlinepollingsystem.exceptions.ValidationDataException
import org.amine.security.polling.onlinepollingsystem.system.audit.admin.model.AdminAuditData
import org.amine.security.polling.onlinepollingsystem.system.audit.user.repository.UserAuditRepository
import org.amine.security.polling.onlinepollingsystem.system.audit.user.model.ArchiveUserAuditData
import org.amine.security.polling.onlinepollingsystem.system.audit.user.model.UserAuditData
import org.amine.security.polling.onlinepollingsystem.system.audit.user.repository.ArchiveUserAuditRepository
import org.amine.security.polling.onlinepollingsystem.system.tools.AppTool
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.util.logging.Logger

@Service
class UserAuditService(
    private val userAuditRepository: UserAuditRepository,
    private val appTool: AppTool,
    private val archiveUserAuditRepository: ArchiveUserAuditRepository
) {

    val logger: Logger? = Logger.getLogger(UserAuditService::class.java.name)

    /**
     *  Kafka
     */
    fun saveAction(
        action: String,
        username: String,
        clientIpAddress: String,
        reason: String,
        timestamp: ZonedDateTime?,
        tokenExpireTime: ZonedDateTime?,
    ) {
        val auditData = UserAuditData()
        auditData.action = action
        auditData.username = username
        auditData.clientIp = clientIpAddress
        auditData.reason = reason
        auditData.timestamp = timestamp
        auditData.tokenExpireTime = tokenExpireTime
        userAuditRepository.save(auditData)
    }


    fun findAdminAuditLastTime(numberOfDays: Long): List<AdminAuditData> {
        if (numberOfDays < 1 || numberOfDays > 90) {
            throw ValidationDataException("The number of days must be between 1 and 90.")
        }
        val startOfTodayZonedDateTime = appTool.getNowTime().minusDays(numberOfDays)
        return userAuditRepository.findByTimestampAfterOrTimestamp(
            startOfTodayZonedDateTime,
            startOfTodayZonedDateTime
        )
    }

    @Scheduled(cron = "0 0 0 */15 * *")
    fun cleanUserAudit() {
        logger!!.info("Start Cleaning User Audit Data")
        val auditDataList = userAuditRepository.findAll().toMutableList()
        for (auditData in auditDataList) {
            if (appTool.getNowTime().isAfter(auditData.timestamp!!.plusMonths(3))) {
                userAuditRepository.delete(auditData)
                val archiveUserAuditData = ArchiveUserAuditData()
                archiveUserAuditData.action = auditData.username
                archiveUserAuditData.username = auditData.username
                archiveUserAuditData.clientIp = auditData.clientIp
                archiveUserAuditData.timestamp = auditData.timestamp
                archiveUserAuditData.tokenExpireTime = auditData.tokenExpireTime
                archiveUserAuditData.tokenExpireTime = auditData.tokenExpireTime
                archiveUserAuditData.auditId = auditData.auditId
                archiveUserAuditRepository.save(archiveUserAuditData)
            }
        }
        logger!!.info("\n End Cleaning User Audit Data")
    }
}