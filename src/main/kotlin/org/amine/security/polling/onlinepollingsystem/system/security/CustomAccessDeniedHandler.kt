package org.amine.security.polling.onlinepollingsystem.system.security

import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import java.io.IOException


@Component
class CustomAccessDeniedHandler : AccessDeniedHandler {
    @Throws(IOException::class, ServletException::class)
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exc: AccessDeniedException?
    ) {
        val auth = SecurityContextHolder.getContext().authentication
        if (auth != null) {
            println(
                "User: " + auth.name
                        + " attempted to access the protected URL: "
                        + request.requestURI
            )
            throw AccessDeniedException(
                "User: " + auth.name
                        + " attempted to access the protected URL: "
                        + request.requestURI
            )
        }

        response.sendRedirect(request.contextPath + "/accessDenied")
    }
}