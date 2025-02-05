package org.amine.security.polling.onlinepollingsystem.system.security.services.detailsservice

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service

@Service
class ApplicationDetailsService(@Qualifier("polling_db") var applicationDetailsService: IApplicationDetailsService) :
    UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        if (username.contains("admin")) {
            return applicationDetailsService.loadAdminDetails(username)
        }
        return applicationDetailsService.loadUserDetails(username)
    }
}