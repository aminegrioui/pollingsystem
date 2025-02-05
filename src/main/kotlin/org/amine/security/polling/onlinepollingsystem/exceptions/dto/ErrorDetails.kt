package org.amine.security.polling.onlinepollingsystem.exceptions.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

class ErrorDetails(
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    val timestamp: LocalDateTime? = null,
    val message: String? = null,
    val details: String? = null
) {

}