package org.amine.security.polling.onlinepollingsystem.dtos.admin.request

class PermissionDTO {
    var permission: String = ""
    var permissionEnabled: Boolean = false
    val id: Long? = null
    var logOutUser: Boolean = false
    var timeOfExpireTimeInMinutes : Long = 5
    var isAdmin: Boolean = false
}