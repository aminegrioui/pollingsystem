package org.amine.security.polling.onlinepollingsystem.system.security.services.detailsservice

import org.springframework.security.core.userdetails.UserDetails

interface IApplicationDetailsService {

    fun loadUserDetails(username:String):UserDetails
    fun loadAdminDetails(username:String):UserDetails
}