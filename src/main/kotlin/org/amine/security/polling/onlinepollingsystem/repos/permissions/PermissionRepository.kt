package org.amine.security.polling.onlinepollingsystem.repos.permissions;

import org.amine.security.polling.onlinepollingsystem.models.permission.Permission
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PermissionRepository : JpaRepository<Permission, Long> {
    fun findByPermission(permission: String): Optional<Permission>
}