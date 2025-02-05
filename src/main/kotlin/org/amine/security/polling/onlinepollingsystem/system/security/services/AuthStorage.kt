package org.amine.security.polling.onlinepollingsystem.system.security.services

import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service

@Service
class AuthStorage {

    /**
     *    This in Kafka
     */

    private val authenticationUsers: MutableMap<String, Authentication> = mutableMapOf()
    private val authenticationAdmins: MutableMap<String, Authentication> = mutableMapOf()

    fun addAuthentication(authentication: Authentication, isAdmin: Boolean) {
        if (isAdmin) {
            authenticationAdmins[authentication.name] = authentication
        } else {
            authenticationUsers[authentication.name] = authentication
        }

    }

    fun getAuthentication(name: String, isAdmin: Boolean): Authentication? {
        return if (isAdmin) authenticationAdmins[name] else authenticationUsers[name]
    }
}