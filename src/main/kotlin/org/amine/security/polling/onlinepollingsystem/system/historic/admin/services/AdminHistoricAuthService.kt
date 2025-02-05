package org.amine.security.polling.onlinepollingsystem.system.historic.admin.services

import org.amine.security.polling.onlinepollingsystem.exceptions.UserLockoutException
import org.amine.security.polling.onlinepollingsystem.models.admin.Admin
import org.amine.security.polling.onlinepollingsystem.repos.admin.AdminRepository
import org.amine.security.polling.onlinepollingsystem.repos.user.UserRepository
import org.amine.security.polling.onlinepollingsystem.system.blacklist.admin.services.AdminBlackListService
import org.amine.security.polling.onlinepollingsystem.system.historic.admin.models.AuthAdminHistoric
import org.amine.security.polling.onlinepollingsystem.system.historic.admin.repository.AuthAdminHistoricRepository
import org.amine.security.polling.onlinepollingsystem.system.tools.AppTool
import org.springframework.stereotype.Service
import java.util.*

@Service
class AdminHistoricAuthService(
    val appTool: AppTool,
    val userRepository: UserRepository,
    val adminBlackListService: AdminBlackListService,
    val authAdminHistoricRepository: AuthAdminHistoricRepository,
    private val adminRepository: AdminRepository,
) {
    fun saveAuthHistoric(authHistoric: AuthAdminHistoric) {
        authAdminHistoricRepository.save(authHistoric)
    }

    fun traceAuthHistoric(username: String, operation: String): AuthAdminHistoric {
        val authAdminHistoric = AuthAdminHistoric()
        authAdminHistoric.username = username
        authAdminHistoric.operation = operation
        authAdminHistoric.operationTimestamp = appTool.getNowTime()
        return authAdminHistoric;
    }

    fun checkAdminAccountLocked(username: String, operation: String) {
        val optionalAdmin: Optional<Admin> = adminRepository.findByUsername(username)
        var admin = Admin()
        if (optionalAdmin.isPresent) {
            admin = optionalAdmin.get()
            if (!admin.isAccountNonLocked) {
                throw UserLockoutException("This Account is locked for 24  Hours ")
            }
        }
        val nowTime = appTool.getNowTime()
        val lastFiveMinutes = nowTime.minusMinutes(5)
        val authHistoricInLastFiveMinutesList = authAdminHistoricRepository.findByUsernameAndTimestampBetweenDate(
            username,
            lastFiveMinutes,
            nowTime,
            operation
        )
        if (authHistoricInLastFiveMinutesList != null) {
            val lastThreeOfAdminHistoric = authHistoricInLastFiveMinutesList.take(3)
            if (lastThreeOfAdminHistoric.size > 2 && lastThreeOfAdminHistoric.none { it?.isSuccessOperation == true }) {
                // save the token of this user as Blacklist
                // instead publish it to kafka: and the rest will be done in another microservice system
                adminBlackListService.saveBlackListEntry(username, "", "BAD_CREDENTIALS")
                admin.isAccountNonLocked = false
                adminRepository.save(admin)
                throw UserLockoutException("You try to log in 3 times with an incorrect password within 5 minutes. This user account is blocked at now for 24 hours.")
            }
        }
    }
}