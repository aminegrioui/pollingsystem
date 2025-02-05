package org.amine.security.polling.onlinepollingsystem.system.historic.admin.repository;

import org.amine.security.polling.onlinepollingsystem.system.historic.admin.models.AuthAdminHistoric
import org.amine.security.polling.onlinepollingsystem.system.historic.user.models.AuthUserHistoric
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.ZonedDateTime

interface AuthAdminHistoricRepository : JpaRepository<AuthAdminHistoric, Long> {
    @Query("select a from AuthAdminHistoric a where a.username=:username and a.operation=:operation and a.operationTimestamp >= :lastFiveMinutes and a.operationTimestamp <= :nowTime  order by a.operationTimestamp desc")
    fun findByUsernameAndTimestampBetweenDate(
        @Param("username") username: String?,
        @Param("lastFiveMinutes") lastFiveMinutes: ZonedDateTime?,
        @Param("nowTime") nowTime: ZonedDateTime?,
        @Param("operation") operation: String?
    ): List<AuthAdminHistoric?>?
}