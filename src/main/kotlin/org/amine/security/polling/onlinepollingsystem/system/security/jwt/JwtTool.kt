package org.amine.security.polling.onlinepollingsystem.system.security.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import org.amine.security.polling.onlinepollingsystem.exceptions.GlobalException
import org.amine.security.polling.onlinepollingsystem.system.security.jwtgen.KeysGenerator
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.InternalAuthenticationServiceException
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import java.security.Signature
import java.security.interfaces.RSAPublicKey
import java.util.*
import java.util.stream.Collectors

@Service
class JwtTool( val keysGenerator: KeysGenerator, var userDetailsService: UserDetailsService) {
    val parseTokenResponse: ParsTokenResponse = ParsTokenResponse();

    fun verifyToken(token: String): DecodedJWT {
        try {
            val partsArray = token.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val parts = partsArray[0] + "." + partsArray[1]
            val sign = partsArray[2]
            val signature = Signature.getInstance("SHA512withRSA")
            signature.initVerify(keysGenerator.pubKey)
            signature.update(parts.toByteArray())
            val isVerified = signature.verify(Base64.getUrlDecoder().decode(sign))
            if (!isVerified) {
                throw BadCredentialsException("Untrusted Jwt: Authentication failed. Please check your credentials and try again. ")
            }
            // Parse the JWT token
            return JWT.require(Algorithm.RSA512(keysGenerator.pubKey as RSAPublicKey, null))
                .build()
                .verify(token)
        } catch (ex: BadCredentialsException) {
            throw BadCredentialsException(ex.message)
        } catch (ex: Exception) {
            throw GlobalException(ex.message)
        }

    }

    fun parseClaimsFromDecodedJWT(decodedJWT: DecodedJWT): ClaimsResponse {
        val claimsResponse = ClaimsResponse()
        val body = decodedJWT.claims
        val authorities = body["authorities"]?.asString()
        val username = body["username"]?.asString()
        try {
            userDetailsService.loadUserByUsername(username);
        } catch (ex: UsernameNotFoundException) {
            throw UsernameNotFoundException(ex.message);
        } catch (ex: UsernameNotFoundException) {
            throw InternalAuthenticationServiceException(ex.message);
        }
        val expireTimeOfRefreshToken = body["exp"]?.asInt()?.let { Date(it * 1000L) }
        val expireTimeOfAccessToken = body["expAccessToken"]?.asInt()?.let { Date(it * 1000L) }
        val issueDateOfToken = body["iat"]?.asInt()?.let { Date(it * 1000L) }
        val arrayStrings: Array<String>
        var simpleGrantedAuthorities: Collection<GrantedAuthority?>? = null
        if (authorities != null) {
            arrayStrings = authorities.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            simpleGrantedAuthorities = Arrays.stream(arrayStrings).map { role: String? ->
                SimpleGrantedAuthority(
                    role
                )
            }.collect(Collectors.toSet())
        }

        claimsResponse.username = username
        claimsResponse.permissions = simpleGrantedAuthorities
        claimsResponse.expireTimeOfRefreshToken = expireTimeOfRefreshToken
        claimsResponse.expireTimeOfAccessToken = expireTimeOfAccessToken
        claimsResponse.issueDateOfToken = issueDateOfToken

        // Auth Info for this Request:
        val userIdValue = body["userId"]?.asInt()
        val adminIdValue = body["adminId"]?.asInt()
        parseTokenResponse.username = username
        if (userIdValue != null) {
            val userId = userIdValue.toLong()
            claimsResponse.id = userId
            parseTokenResponse.id = userId
        } else {
            val adminId = adminIdValue!!.toLong()
            claimsResponse.id = adminId
            claimsResponse.isAdmin = true
            parseTokenResponse.id = adminId
            parseTokenResponse.isUser = authorities?.contains("ROLE_USER") ?: false
            parseTokenResponse.isAdmin = authorities?.contains("ROLE_ADMIN") ?: false
        }
        return claimsResponse
    }

}