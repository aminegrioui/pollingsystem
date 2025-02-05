package org.amine.security.polling.onlinepollingsystem.repos.admin

import org.amine.security.polling.onlinepollingsystem.models.admin.Admin
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*


interface AdminRepository : JpaRepository<Admin, Long> {
    fun findByUsername(username: String?): Optional<Admin>;
    fun findByEmail(email: String?): Optional<Admin>
}