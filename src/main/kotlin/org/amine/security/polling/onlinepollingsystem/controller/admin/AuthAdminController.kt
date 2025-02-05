package org.amine.security.polling.onlinepollingsystem.controller.admin

import jakarta.servlet.http.HttpServletRequest
import org.amine.security.polling.onlinepollingsystem.dtos.admin.request.AdminRegisterDto
import org.amine.security.polling.onlinepollingsystem.dtos.user.request.UserLoginDto
import org.amine.security.polling.onlinepollingsystem.services.admin.AuthAdminService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/polling/v1/admin/auth")
class AuthAdminController(val adminService: AuthAdminService) {

    @PostMapping("/registerAdmin")
    fun registerUser(
        @RequestBody adminRegisterDto: AdminRegisterDto,
        requestHeader: HttpServletRequest
    ): ResponseEntity<*>? {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(adminService.registerAdmin(adminRegisterDto, requestHeader))
    }

    @PostMapping("/loginAdmin")
    fun loginUser(@RequestBody userLoginDto: UserLoginDto, requestHeader: HttpServletRequest): ResponseEntity<*>? {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.loginAdmin(userLoginDto, requestHeader))
    }
}