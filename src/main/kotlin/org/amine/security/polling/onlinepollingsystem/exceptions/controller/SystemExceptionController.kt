package org.amine.security.polling.onlinepollingsystem.exceptions.controller

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import org.amine.security.polling.onlinepollingsystem.exceptions.*
import org.amine.security.polling.onlinepollingsystem.exceptions.dto.ErrorDetails
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.InternalAuthenticationServiceException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import java.time.LocalDateTime

@ControllerAdvice
class SystemExceptionController {

    @ExceptionHandler(ValidationDataException::class)
    fun handelValidationDataException(
        exception: ValidationDataException,
        webRequest: WebRequest
    ): ResponseEntity<ErrorDetails> {
        val errorDetails = ErrorDetails(LocalDateTime.now(), exception.message, webRequest.getDescription(false))
        return ResponseEntity<ErrorDetails>(errorDetails, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(EmailValidationException::class)
    fun handelEmailValidationException(
        exception: EmailValidationException,
        webRequest: WebRequest
    ): ResponseEntity<ErrorDetails> {
        val errorDetails = ErrorDetails(LocalDateTime.now(), exception.message, webRequest.getDescription(false))
        return ResponseEntity<ErrorDetails>(errorDetails, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(InvalidFormatException::class)
    fun handelInvalidFormatException(
        exception: InvalidFormatException,
        webRequest: WebRequest
    ): ResponseEntity<ErrorDetails> {
        val errorDetails = ErrorDetails(
            LocalDateTime.now(),
            "A datetime must have this format yyyy-MM-dd'T'HH:mm:ss",
            webRequest.getDescription(false)
        )
        return ResponseEntity<ErrorDetails>(errorDetails, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(AlreadyExistUserException::class)
    fun handelAlreadyExistUserException(
        exception: AlreadyExistUserException,
        webRequest: WebRequest
    ): ResponseEntity<ErrorDetails> {
        val errorDetails = ErrorDetails(LocalDateTime.now(), exception.message, webRequest.getDescription(false))
        return ResponseEntity<ErrorDetails>(errorDetails, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(RemoveParticipateRightException::class)
    fun handelRemoveParticipateRightException(
        exception: RemoveParticipateRightException,
        webRequest: WebRequest
    ): ResponseEntity<ErrorDetails> {
        val errorDetails = ErrorDetails(LocalDateTime.now(), exception.message, webRequest.getDescription(false))
        return ResponseEntity<ErrorDetails>(errorDetails, HttpStatus.FORBIDDEN)
    }

    @ExceptionHandler(InternalAuthenticationServiceException::class)
    fun handelInternalAuthenticationServiceException(
        exception: InternalAuthenticationServiceException,
        webRequest: WebRequest
    ): ResponseEntity<ErrorDetails> {
        val errorDetails = ErrorDetails(LocalDateTime.now(), exception.message, webRequest.getDescription(false))
        return ResponseEntity<ErrorDetails>(errorDetails, HttpStatus.UNAUTHORIZED)
    }

    @ExceptionHandler(BadCredentialsException::class)
    fun handelBadCredentialsException(
        exception: BadCredentialsException,
        webRequest: WebRequest
    ): ResponseEntity<ErrorDetails> {
        val errorDetails = ErrorDetails(LocalDateTime.now(), exception.message, webRequest.getDescription(false))
        return ResponseEntity(errorDetails, HttpStatus.UNAUTHORIZED)
    }
    @ExceptionHandler(AccessDeniedException::class)
    fun handelAccessDeniedException(
        exception: AccessDeniedException,
        webRequest: WebRequest
    ): ResponseEntity<ErrorDetails> {
        val errorDetails = ErrorDetails(LocalDateTime.now(), exception.message, webRequest.getDescription(false))
        return ResponseEntity(errorDetails, HttpStatus.FORBIDDEN)
    }

    @ExceptionHandler(UserNameNotFoundException::class)
    fun handelInternalAuthenticationServiceException(
        exception: UserNameNotFoundException,
        webRequest: WebRequest
    ): ResponseEntity<ErrorDetails> {
        val errorDetails = ErrorDetails(LocalDateTime.now(), exception.message, webRequest.getDescription(false))
        return ResponseEntity(errorDetails, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(UserLockoutException::class)
    fun handelUserLockoutException(
        exception: UserLockoutException,
        webRequest: WebRequest
    ): ResponseEntity<ErrorDetails> {
        val errorDetails = ErrorDetails(LocalDateTime.now(), exception.message, webRequest.getDescription(false))
        return ResponseEntity(errorDetails, HttpStatus.UNAUTHORIZED)
    }

    @ExceptionHandler(GlobalException::class)
    fun handelGlobalException(
        exception: GlobalException,
        webRequest: WebRequest
    ): ResponseEntity<ErrorDetails> {
        val errorDetails = ErrorDetails(LocalDateTime.now(), exception.message, webRequest.getDescription(false))
        return ResponseEntity<ErrorDetails>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handelResourceNotFoundException(
        exception: ResourceNotFoundException,
        webRequest: WebRequest
    ): ResponseEntity<ErrorDetails> {
        val errorDetails = ErrorDetails(LocalDateTime.now(), exception.message, webRequest.getDescription(false))
        return ResponseEntity<ErrorDetails>(errorDetails, HttpStatus.NOT_FOUND)
    }
}