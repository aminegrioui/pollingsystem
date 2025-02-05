package org.amine.security.polling.onlinepollingsystem.system.tools

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component
import org.springframework.http.HttpHeaders
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

@Component
class AppTool {

    fun validateUsername(username: String): Boolean {
        return username.matches(Regex("^[a-zA-Z]*$")) && username.length > 7
    }

    fun validatePassword(password: String): Boolean {
        return password.matches(Regex("^(?=.*[a-zA-Z])(?=.*[0-9])[A-Za-z0-9]+$")) && password.length > 9;
    }

    fun checkValidationOfGivenEmail(newEmail: String): Boolean {
        val EMAIL_VERIFICATION = "^([\\w-\\.]+){1,64}@([\\w&&[^_]]+){2,255}.[a-z]{2,}$"
        return newEmail.matches(EMAIL_VERIFICATION.toRegex())
    }

    fun getNowTime(): ZonedDateTime {
        return ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("Europe/Berlin"))
    }

    fun validateDate(date: String): Boolean {
        val regex =
            "^\\d\\d\\d\\d-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01]) (00|[0-9]|1[0-9]|2[0-3]):([0-9]|[0-5][0-9]):([0-9]|[0-5][0-9])\$";
        return Pattern.matches(regex, date)
    }

    fun toRightDateFormat(dateAsString: String): ZonedDateTime {
        val localDateTime = LocalDateTime.parse(dateAsString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        return localDateTime.atZone(ZoneId.of("Europe/Berlin"))
    }

    fun setOfUserPermissions(): Set<String> {
        return mutableSetOf("ROLE_USER", "WRITE_USER", "WRITE_POLE", "PARTICIPATE_POLE")
    }

    fun getUerIpAddress(request: HttpServletRequest): String {
        return request.remoteAddr
    }

    fun getUerIpAddress(headers: HttpHeaders): String {

        // Check if X-Forwarded-For header is present
        val xForwardedFor: String = headers.getFirst("X-Forwarded-For").toString()
        if (!xForwardedFor.isEmpty()) {
            // The X-Forwarded-For header can contain a comma-separated list of IP addresses,
            // the client's address being first, so we split and return the first one
            return xForwardedFor.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()[0].trim { it <= ' ' }
        }


        // If X-Forwarded-For header is not present, fallback to Remote-Addr header
        return headers.getFirst("Remote-Addr")!!
    }

}