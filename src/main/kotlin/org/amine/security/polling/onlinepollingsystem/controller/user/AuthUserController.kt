package org.amine.security.polling.onlinepollingsystem.controller.user

import jakarta.servlet.http.HttpServletRequest
import org.amine.security.polling.onlinepollingsystem.dtos.user.request.UserLoginDto
import org.amine.security.polling.onlinepollingsystem.dtos.user.request.UserRegisterDto
import org.amine.security.polling.onlinepollingsystem.services.user.AuthUserService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/polling/v1/auth")
class AuthUserController(val userServiceAuth: AuthUserService) {

    @PostMapping("/registerUser")
    fun registerUser(@RequestBody userDto: UserRegisterDto, requestHeader: HttpServletRequest): ResponseEntity<*>? {
        return ResponseEntity.status(HttpStatus.CREATED).body(userServiceAuth.registerUser(userDto, requestHeader))
    }

    @PostMapping("/loginUser")
    fun loginUser(@RequestBody userLoginDto: UserLoginDto, requestHeader: HttpServletRequest): ResponseEntity<*>? {
        return ResponseEntity.status(HttpStatus.CREATED).body(userServiceAuth.loginUser(userLoginDto, requestHeader))
    }
}