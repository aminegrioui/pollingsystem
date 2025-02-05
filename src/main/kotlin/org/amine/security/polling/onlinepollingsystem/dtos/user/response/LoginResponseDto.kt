package org.amine.security.polling.onlinepollingsystem.dtos.user.response

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.ZonedDateTime

class LoginResponseDto {
    var jwtAccessToken: String =""
    var jwtRefreshToken: String =""
    @JsonIgnore
    var tokenExpireTime: ZonedDateTime? = null
}