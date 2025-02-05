package org.amine.security.polling.onlinepollingsystem.system.blacklist.admin.services

import com.google.common.base.Strings
import jakarta.security.auth.message.AuthException
import jakarta.servlet.http.HttpServletRequest
import org.amine.security.polling.onlinepollingsystem.exceptions.BlackListException
import org.amine.security.polling.onlinepollingsystem.exceptions.UserLockoutException
import org.amine.security.polling.onlinepollingsystem.repos.admin.AdminRepository
import org.amine.security.polling.onlinepollingsystem.repos.user.UserRepository
import org.amine.security.polling.onlinepollingsystem.system.blacklist.user.models.AdminBlackListEntry
import org.amine.security.polling.onlinepollingsystem.system.blacklist.user.repositories.AdminBlackListRepository
import org.amine.security.polling.onlinepollingsystem.system.tools.AppTool
import org.springframework.http.HttpHeaders
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.*
import java.util.logging.Logger
import kotlin.system.measureTimeMillis

@Service
class AdminBlackListService(
    val adminBlackListRepository: AdminBlackListRepository,
    var appTool: AppTool,
    val userRepository: UserRepository,
    private val adminRepository: AdminRepository
) {

    val logger = Logger.getLogger(AdminBlackListService::class.java.name)
    fun saveBlackListEntry(username: String, token: String, cause: String) {
        val adminBlackListEntry = AdminBlackListEntry()
        if (!Strings.isNullOrEmpty(username)) {
            adminBlackListEntry.username = username
        }
        if (!Strings.isNullOrEmpty(token)) {
            adminBlackListEntry.token = token.substring(token.indexOf('.') + 1, 132)
        }
        adminBlackListEntry.expireTime = appTool.getNowTime().plusMinutes(5)
        adminBlackListEntry.cause = cause
        adminBlackListRepository.save(adminBlackListEntry)
    }

    fun checkTokenOrUsernameInBlackList(username: String?, requestHeader: HttpHeaders, request: HttpServletRequest?) {

        val blacklistUserNameEntry: Optional<AdminBlackListEntry> =
            adminBlackListRepository.findByUsername(username)
        if (blacklistUserNameEntry.isPresent && blacklistUserNameEntry.get().cause == "DISABLE_USER") {
            throw UserLockoutException("This Account  is disabled. Contact Administration")
        }
        if (blacklistUserNameEntry.isPresent && blacklistUserNameEntry.get().expireTime?.isAfter(appTool.getNowTime()) == true) {
            throw UserLockoutException("This Account is locked for 24 Hours ")
        }
        val token: String
        if (request != null) {
            token = request.getHeader("Authorization").replace("Bearer", "")
        } else {
            val values = requestHeader["Authorization"]
            if (values.isNullOrEmpty()) {
                throw AuthException("This request is unauthorized. It has no Token !!! ")
            }
            token = values[0].replace("Bearer", "")
        }


        if (Strings.isNullOrEmpty(token)) {
            throw AuthException("This request is unauthorized. It has no Token !!! ")
        }

        val bodyOfToken = token.substring(token.indexOf('.') + 1, 132)
        val blacklistTokenEntry: Optional<AdminBlackListEntry> = adminBlackListRepository.findByToken(bodyOfToken)
        if (blacklistTokenEntry.isPresent && blacklistTokenEntry.get().expireTime?.isAfter(appTool.getNowTime()) == true) {
            throw BlackListException("The token is blacklisted and invalid. Please log in again or contact support")
        }
    }

    @Scheduled(cron = "0 */3 * * * *", zone = "Europe/Berlin")
    fun scanBlackList() {
        logger.info("START_SCAN_BLACK_LIST: ${appTool.getNowTime()}")
        val elapsedTime = measureTimeMillis {
            trackBlackListWithParallelStream()
        }
        logger.info("END_SCAN_BLACK_LIST: elapsedTime: $elapsedTime ms,  ${appTool.getNowTime()}")
    }

    fun trackBlackListWithParallelStream() {
        val blackListEntries = adminBlackListRepository.findAll()
        blackListEntries.parallelStream().forEach(this::processBlacklistEntry)
    }

    fun processBlacklistEntry(entry: AdminBlackListEntry) {
        val nowTime = appTool.getNowTime()
        if (entry.expireTime?.isBefore(nowTime) == true || entry.expireTime?.isEqual(nowTime) == true) {
            if (!Strings.isNullOrEmpty(entry.username)) {
                val optionalAdmin = adminRepository.findByUsername(entry.username)
                if (optionalAdmin.isPresent && !optionalAdmin.get().isDeleted) {
                    optionalAdmin.get().isAccountNonLocked = true
                    adminRepository.save(optionalAdmin.get())
                    logger.info("Admin with username:  ${entry.username} is again active.  expireTime: ${entry.expireTime}, now: $nowTime ")
                    // sendEmail Aktivation User Account
                }
            } else {
                logger.info("The removed blackListEntry:  ${entry.token}, expireTime: ${entry.expireTime}, now: $nowTime ")
            }

            adminBlackListRepository.delete(entry)
        }
    }
}