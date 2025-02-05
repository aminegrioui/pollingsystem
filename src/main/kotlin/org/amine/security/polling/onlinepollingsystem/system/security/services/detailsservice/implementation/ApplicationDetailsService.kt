package org.amine.security.polling.onlinepollingsystem.system.security.services.detailsservice.implementation

import org.amine.security.polling.onlinepollingsystem.models.admin.Admin
import org.amine.security.polling.onlinepollingsystem.models.users.User
import org.amine.security.polling.onlinepollingsystem.repos.admin.AdminRepository
import org.amine.security.polling.onlinepollingsystem.repos.user.UserRepository
import org.amine.security.polling.onlinepollingsystem.system.security.models.ApplicationUserDetails
import org.amine.security.polling.onlinepollingsystem.system.security.services.detailsservice.IApplicationDetailsService
import org.springframework.security.authentication.InternalAuthenticationServiceException
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.util.*

@Service("polling_db")
class ApplicationDetailsService(
    val userRepository: UserRepository,
    private val adminRepository: AdminRepository
) :
    IApplicationDetailsService {

    override fun loadUserDetails(username: String): UserDetails {
        val optionalUserUsingUsername: Optional<User> = userRepository.findByUsername(username);

        if (optionalUserUsingUsername.isEmpty || optionalUserUsingUsername.get().isDeleted) {
            throw InternalAuthenticationServiceException("A user with this username: $username is not found ")
        }
        if (!optionalUserUsingUsername.get().isEnabled) {
            throw InternalAuthenticationServiceException("The user with username $username is disabled. Contact Administration")
        }

        val user = optionalUserUsingUsername.get()
        val permissions: MutableCollection<GrantedAuthority> = user.permissions
            .map { permission -> SimpleGrantedAuthority(permission.permission) }
            .toMutableList()
            .toMutableSet()



        return ApplicationUserDetails(
            username,
            user.password,
            permissions,
            user.isAccountNonExpired,
            user.isEnabled,
            user.isCredentialsNonExpired,
            user.isAccountNonLocked,
            user.id
        )
    }

    override fun loadAdminDetails(username: String): UserDetails {
        val newUsername = username.replace("admin_", "")
        val optionalAdminUsingUsername: Optional<Admin> = adminRepository.findByUsername(newUsername);

        if (optionalAdminUsingUsername.isEmpty || optionalAdminUsingUsername.get().isDeleted) {
            throw InternalAuthenticationServiceException("A user with this username: $newUsername is not found ")
        }
        if (!optionalAdminUsingUsername.get().isEnabled) {
            throw InternalAuthenticationServiceException("The user with username $newUsername is disabled. Contact Administration")
        }

        val admin = optionalAdminUsingUsername.get()
        val permissions: MutableCollection<GrantedAuthority> = admin.permissions
            .map { permission -> SimpleGrantedAuthority(permission.permission) }
            .toMutableList()
            .toMutableSet()



        return ApplicationUserDetails(
            newUsername,
            admin.password,
            permissions,
            admin.isAccountNonExpired,
            admin.isEnabled,
            admin.isCredentialsNonExpired,
            admin.isAccountNonLocked,
            admin.adminId
        )
    }
}