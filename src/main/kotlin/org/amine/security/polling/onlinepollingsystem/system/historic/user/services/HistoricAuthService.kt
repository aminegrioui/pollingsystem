package org.amine.security.polling.onlinepollingsystem.system.historic.user.services

import org.amine.security.polling.onlinepollingsystem.exceptions.UserLockoutException
import org.amine.security.polling.onlinepollingsystem.models.users.User
import org.amine.security.polling.onlinepollingsystem.repos.user.UserRepository
import org.amine.security.polling.onlinepollingsystem.system.blacklist.user.services.BlackListService
import org.amine.security.polling.onlinepollingsystem.system.historic.user.models.AuthUserHistoric
import org.amine.security.polling.onlinepollingsystem.system.historic.user.repository.AuthUserHistoricRepository
import org.amine.security.polling.onlinepollingsystem.system.tools.AppTool
import org.springframework.stereotype.Service
import java.util.*

@Service
class HistoricAuthService(
    val appTool: AppTool,
    val authUserHistoricRepository: AuthUserHistoricRepository,
    val userRepository: UserRepository,
    private val blackListService: BlackListService
) {
    fun saveAuthHistoric(authHistoric: AuthUserHistoric) {
        authUserHistoricRepository.save(authHistoric)
    }

    fun traceAuthHistoric(username: String, operation: String): AuthUserHistoric {
        val authUserHistoric = AuthUserHistoric()
        authUserHistoric.username = username
        authUserHistoric.operation = operation
        authUserHistoric.operationTimestamp = appTool.getNowTime()
        return authUserHistoric;
    }

    fun checkUserAccountLocked(username: String, operation: String) {
        val optionalUser: Optional<User> = userRepository.findByUsername(username)
        var user = User()
        if (optionalUser.isPresent) {
            user = optionalUser.get()
            if (!user.isAccountNonLocked) {
                throw UserLockoutException("This Account is locked for 24  Hours ")
            }
        }
        val nowTime = appTool.getNowTime()
        val lastFiveMinutes = nowTime.minusMinutes(5)
        val authHistoricInLastFiveMinutesList = authUserHistoricRepository.findByUsernameAndTimestampBetweenDate(
            username,
            lastFiveMinutes,
            nowTime,
            operation
        )
        if (authHistoricInLastFiveMinutesList != null) {
            val lastThreeOfUserHistoric = authHistoricInLastFiveMinutesList.take(3)
            if (lastThreeOfUserHistoric.size > 2 && lastThreeOfUserHistoric.none { it?.isSuccessOperation == true }) {
                // save the token of this user as Blacklist
                // instead publish it to kafka: and the rest will be done in another microservice system
                blackListService.saveBlackListEntry(username, "", "BAD_CREDENTIALS")
                user.isAccountNonLocked = false
                userRepository.save(user)
                throw UserLockoutException("You try to log in 3 times with an incorrect password within 5 minutes. This user account is blocked at now for 24 hours.")
            }
        }
    }
}