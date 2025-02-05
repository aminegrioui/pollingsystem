package org.amine.security.polling.onlinepollingsystem.system.blacklist.user.repositories;


import org.amine.security.polling.onlinepollingsystem.system.blacklist.user.models.AdminBlackListEntry
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface AdminBlackListRepository : JpaRepository<AdminBlackListEntry, Long> {
    fun findByUsername(username: String?): Optional<AdminBlackListEntry>
    fun findByToken(token: String): Optional<AdminBlackListEntry>
}