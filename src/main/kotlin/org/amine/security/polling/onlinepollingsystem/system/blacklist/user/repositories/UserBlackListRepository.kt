package org.amine.security.polling.onlinepollingsystem.system.blacklist.user.repositories;


import org.amine.security.polling.onlinepollingsystem.system.blacklist.user.models.UserBlackListEntry
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UserBlackListRepository : JpaRepository<UserBlackListEntry, Long> {
    fun findByUsername(username: String?): Optional<UserBlackListEntry>
    fun findByToken(token: String): Optional<UserBlackListEntry>
}