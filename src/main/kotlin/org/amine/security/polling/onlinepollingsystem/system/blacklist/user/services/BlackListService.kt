package org.amine.security.polling.onlinepollingsystem.system.blacklist.user.services

import com.google.common.base.Strings
import jakarta.security.auth.message.AuthException
import jakarta.servlet.http.HttpServletRequest
import org.amine.security.polling.onlinepollingsystem.exceptions.BlackListException
import org.amine.security.polling.onlinepollingsystem.exceptions.UserLockoutException
import org.amine.security.polling.onlinepollingsystem.repos.user.UserRepository
import org.amine.security.polling.onlinepollingsystem.system.blacklist.admin.services.AdminBlackListService
import org.amine.security.polling.onlinepollingsystem.system.blacklist.user.models.UserBlackListEntry
import org.amine.security.polling.onlinepollingsystem.system.blacklist.user.repositories.UserBlackListRepository
import org.amine.security.polling.onlinepollingsystem.system.tools.AppTool
import org.springframework.http.HttpHeaders
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.*
import java.util.logging.Logger
import kotlin.system.measureTimeMillis

@Service
class BlackListService(
    var blackListRepository: UserBlackListRepository,
    var appTool: AppTool, val userRepository: UserRepository
) {

    val logger = Logger.getLogger(AdminBlackListService::class.java.name)
    fun saveBlackListEntry(username: String, token: String, cause: String) {
        val userBlackList = UserBlackListEntry()
        if (!Strings.isNullOrEmpty(username) && blackListRepository.findByUsername(username).isEmpty) {
            userBlackList.username = username
        }
        if (!Strings.isNullOrEmpty(token)) {
            userBlackList.token = token.substring(token.indexOf('.') + 1, 132)
        }
        userBlackList.expireTime = appTool.getNowTime().plusMinutes(5)
        userBlackList.cause = cause
        blackListRepository.save(userBlackList)
    }

    fun saveBlackListEntry(username: String, token: String, cause: String, timeOfExpireTimeInMinutes: Long) {
        val userBlackList = UserBlackListEntry()
        if (!Strings.isNullOrEmpty(username) && blackListRepository.findByUsername(username).isEmpty) {
            userBlackList.username = username
        }
        if (!Strings.isNullOrEmpty(token)) {
            userBlackList.token = token.substring(token.indexOf('.') + 1, 132)
        }
        userBlackList.expireTime = appTool.getNowTime().plusMinutes(timeOfExpireTimeInMinutes)
        userBlackList.cause = cause
        blackListRepository.save(userBlackList)
    }

    fun checkTokenOrUsernameInBlackList(username: String?, requestHeader: HttpHeaders, request: HttpServletRequest?) {

        val blacklistUserNameEntry: Optional<UserBlackListEntry> =
            blackListRepository.findByUsername(username)
        if (blacklistUserNameEntry.isPresent && blacklistUserNameEntry.get().cause == "DISABLE_USER") {
            throw UserLockoutException("This Account  is disabled. Contact Administration")
        }
        if (blacklistUserNameEntry.isPresent && blacklistUserNameEntry.get().expireTime?.isAfter(appTool.getNowTime()) == true) {
            throw UserLockoutException("Access to this Account is restricted until ${blacklistUserNameEntry.get().expireTime} ")
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
        val blacklistTokenEntry: Optional<UserBlackListEntry> = blackListRepository.findByToken(bodyOfToken)
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
        val blackListEntries = blackListRepository.findAll()
        blackListEntries.parallelStream().forEach(this::processBlacklistEntry)
    }

    fun processBlacklistEntry(entry: UserBlackListEntry) {
        val nowTime = appTool.getNowTime()
        if (entry.expireTime?.isBefore(nowTime) == true || entry.expireTime?.isEqual(nowTime) == true) {
            if (!Strings.isNullOrEmpty(entry.username)) {
                val optionalUser = userRepository.findByUsername(entry.username)
                if (optionalUser.isPresent && !optionalUser.get().isDeleted) {
                    optionalUser.get().isAccountNonLocked = true
                    userRepository.save(optionalUser.get())
                    logger.info("User with username:  ${entry.username} is again active.  expireTime: ${entry.expireTime}, now: $nowTime ")
                    // sendEmail Aktivation User Account
                }
            } else {
                logger.info("The removed blackListEntry:  ${entry.token}, expireTime: ${entry.expireTime}, now: $nowTime ")
            }

            blackListRepository.delete(entry)
        }
    }
}