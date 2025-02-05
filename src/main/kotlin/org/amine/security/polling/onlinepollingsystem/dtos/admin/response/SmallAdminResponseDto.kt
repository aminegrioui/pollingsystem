package org.amine.security.polling.onlinepollingsystem.dtos.admin.response

class SmallAdminResponseDto {
    var adminId: Long = 0
    var username: String? = null
    var email: String? = null
    var users: List<String> = mutableListOf()
    var permissions: List<String> = mutableListOf()
}