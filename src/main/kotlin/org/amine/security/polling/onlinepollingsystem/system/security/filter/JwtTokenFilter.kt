package org.amine.security.polling.onlinepollingsystem.system.security.filter

import com.google.common.base.Strings
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.amine.security.polling.onlinepollingsystem.system.security.jwt.JwtTool
import org.amine.security.polling.onlinepollingsystem.system.security.jwtgen.JwtGenerator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerExceptionResolver
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.SignatureException

@Service
class JwtTokenFilter(
    val jwtTool: JwtTool,
    val jwtGenerator: JwtGenerator,
    @Autowired @Qualifier("handlerExceptionResolver")
    var resolver: HandlerExceptionResolver? = null
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        var authorization = request.getHeader("Authorization_Refresh_Token");
        val token: String
        var isRefreshToken = false

        if (!Strings.isNullOrEmpty(authorization)) {
            token = authorization.replace("refreshToken ","");
            isRefreshToken = true;
        } else {
            authorization = request.getHeader("Authorization")
            if (Strings.isNullOrEmpty(authorization) || !authorization.startsWith("Bearer ")) {
                return filterChain.doFilter(request, response);
            }
            token = authorization.replace("Bearer ","");
        }

        try {
            val jwsClaims = jwtTool.verifyToken(token)
            val parsTokenResponse = jwtTool.parseClaimsFromDecodedJWT(jwsClaims)

            val username = parsTokenResponse.username
            val grantedAuthorities = parsTokenResponse.permissions
            if (isRefreshToken) {
                val loginResponseDto = jwtGenerator.generateNewAccessJwtToken(parsTokenResponse)
                response.addHeader("accessToken", loginResponseDto.jwtAccessToken)
                response.addHeader("refreshToken", loginResponseDto.jwtRefreshToken)
            }
            val usernamePasswordAuthenticationToken =
                UsernamePasswordAuthenticationToken(username, null, grantedAuthorities)
            SecurityContextHolder.getContext().authentication = usernamePasswordAuthenticationToken
            filterChain.doFilter(request, response)
        } catch (e: NoSuchAlgorithmException) {
            resolver!!.resolveException(request, response, null, e)
        } catch (e: InvalidKeyException) {
            resolver!!.resolveException(request, response, null, e)
        } catch (e: SignatureException) {
            resolver!!.resolveException(request, response, null, e)
        } catch (e: BadCredentialsException) {
            resolver!!.resolveException(request, response, null, e)
        } catch (ex: Exception) {
            // Handle other exceptions
            // You can uncomment the next line if you want to rethrow the exception as a GlobalException
            resolver!!.resolveException(request, response, null, ex)
        }
    }
}