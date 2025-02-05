package org.amine.security.polling.onlinepollingsystem.system.controller

import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/polling/v1/refreshToken")
class RefreshTokenController {

    @GetMapping
    fun getNewAccessToken(response: HttpServletResponse): RefreshTokenResponseDto {
        val accessToken = response.getHeader("accessToken")
        val refreshToken = response.getHeader("refreshToken")
        return RefreshTokenResponseDto(accessToken, refreshToken)
    }
}