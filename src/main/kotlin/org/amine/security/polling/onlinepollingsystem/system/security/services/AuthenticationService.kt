package org.amine.security.polling.onlinepollingsystem.system.security.services


import org.springframework.stereotype.Service

@Service
class AuthenticationService(private val authStorage: AuthStorage) {
    fun isAuthenticated(username: String): Boolean {
        val authentication = authStorage.getAuthentication(username,false)
        return authentication != null
    }
}