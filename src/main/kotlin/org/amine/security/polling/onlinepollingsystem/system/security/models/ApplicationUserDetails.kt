package org.amine.security.polling.onlinepollingsystem.system.security.models

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class ApplicationUserDetails(
    var userName: String,
    var pass: String,
    var grantedAuthorities: MutableCollection<GrantedAuthority>?,
    var accountNonExpired: Boolean,
    var enabled: Boolean,
    var credentialsNonExpired: Boolean,
    var accountNonLocked: Boolean,
    var identifier: Long
) : UserDetails {

    fun getId(): Long {
        return identifier;
    }

    override fun getAuthorities(): MutableCollection<GrantedAuthority>? {
        return grantedAuthorities;
    }

    override fun getPassword(): String? {
        return pass
    }

    override fun getUsername(): String {
        return userName;
    }

    override fun isAccountNonExpired(): Boolean {
        return accountNonExpired;
    }

    override fun isAccountNonLocked(): Boolean {
        return accountNonLocked
    }

    override fun isCredentialsNonExpired(): Boolean {
        return credentialsNonExpired
    }

    override fun isEnabled(): Boolean {
        return enabled;
    }
}