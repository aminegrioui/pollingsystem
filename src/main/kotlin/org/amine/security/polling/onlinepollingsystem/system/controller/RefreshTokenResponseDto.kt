package org.amine.security.polling.onlinepollingsystem.system.controller

class RefreshTokenResponseDto(
    val jwtAccessToken: String ="",
    val jwtRefreshToken: String =""
)