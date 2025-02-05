package org.amine.security.polling.onlinepollingsystem.system.security.jwtgen

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm

import org.amine.security.polling.onlinepollingsystem.dtos.user.response.LoginResponseDto
import org.amine.security.polling.onlinepollingsystem.exceptions.MissingUsernameException
import org.amine.security.polling.onlinepollingsystem.models.users.User
import org.amine.security.polling.onlinepollingsystem.repos.user.UserRepository
import org.amine.security.polling.onlinepollingsystem.system.security.jwt.ClaimsResponse
import org.amine.security.polling.onlinepollingsystem.system.security.models.ApplicationUserDetails
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Service
import java.io.IOException
import java.security.NoSuchAlgorithmException
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*
import lombok.extern.log4j.Log4j
import org.amine.security.polling.onlinepollingsystem.exceptions.ResourceNotFoundException
import org.amine.security.polling.onlinepollingsystem.models.admin.Admin
import org.amine.security.polling.onlinepollingsystem.repos.admin.AdminRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.util.stream.Collectors

@Service
@Log4j
class JwtGenerator(
    val keysGenerator: KeysGenerator,
    val userRepository: UserRepository,
    private val adminRepository: AdminRepository,
) {

    fun generateAccessAndRefreshToken(authentication: Authentication): LoginResponseDto {
        val grantedAuthoritiesAsString = getGrantedAuthoritiesAsString(authentication.authorities)
        val id = (authentication.principal as ApplicationUserDetails).getId()
        val username = (authentication.principal as ApplicationUserDetails).userName
        val isAdmin = grantedAuthoritiesAsString.contains("ROLE_ADMIN")
        val expireTimeOfAccessToken = Date.from(Instant.now().plus(30, ChronoUnit.MINUTES))
        val accessToken: String =
            buildToken(isAdmin, id, username, grantedAuthoritiesAsString, Date(), expireTimeOfAccessToken, null)
        val expireTimeOfRefreshToken = Date.from(Instant.now().plus(24, ChronoUnit.HOURS))

        val refreshToken: String = buildToken(
            isAdmin,
            id,
            username,
            grantedAuthoritiesAsString,
            Date(),
            expireTimeOfAccessToken,
            expireTimeOfRefreshToken
        )
        val loginResponseDto = LoginResponseDto()
        loginResponseDto.jwtAccessToken = accessToken
        loginResponseDto.jwtRefreshToken = refreshToken
        loginResponseDto.tokenExpireTime = expireTimeOfAccessToken.toInstant().atZone(ZoneId.systemDefault())
        return loginResponseDto
    }

    fun buildToken(
        isAdmin: Boolean,
        id: Long,
        username: String?,
        grantedAuthoritiesAsString: String,
        iat: Date,
        expireTimeOfAccessToken: Date,
        expireTimeOfRefreshToken: Date?
    ): String {
        val claimId = if (isAdmin) "adminId" else "userId"
        var newUsername = username
        if (isAdmin) {
            newUsername = "admin_$username"
        }
        if (expireTimeOfRefreshToken != null) {
            return JWT.create()
                .withClaim(claimId, id)
                .withClaim("username", newUsername)
                .withClaim("authorities", grantedAuthoritiesAsString)
                .withClaim("expAccessToken", expireTimeOfAccessToken)
                .withIssuedAt(iat)
                .withExpiresAt(expireTimeOfRefreshToken)
                .sign(
                    Algorithm.RSA512(
                        keysGenerator.pubKey as RSAPublicKey?,
                        keysGenerator.priKey as RSAPrivateKey?
                    )
                )
        }
        return JWT.create()
            .withClaim(claimId, id)
            .withClaim("username", newUsername)
            .withClaim("authorities", grantedAuthoritiesAsString)
            .withIssuedAt(iat)
            .withExpiresAt(expireTimeOfAccessToken)
            .sign(
                Algorithm.RSA512(
                    keysGenerator.pubKey as RSAPublicKey?,
                    keysGenerator.priKey as RSAPrivateKey?
                )
            )
    }

    @Throws(IOException::class, NoSuchAlgorithmException::class)
    fun generateNewAccessJwtToken(
        parseClaimsFromResponse: ClaimsResponse,
    ): LoginResponseDto {

        val username: String? = parseClaimsFromResponse.username;
        val expireTimeOfFreshToken: Date? = parseClaimsFromResponse.expireTimeOfRefreshToken
        val expireTimeOfAccessToken = parseClaimsFromResponse.expireTimeOfAccessToken
        var authoritiesAsString = ""
        val grantedAuthorities: Collection<GrantedAuthority>?
        var id: Long? = null
        val isAdmin = parseClaimsFromResponse.isAdmin
        val iat = parseClaimsFromResponse.issueDateOfToken


        if (isAdmin) {
            val optionalAdmin: Optional<Admin> = adminRepository.findByUsername(username)
            if (optionalAdmin.isEmpty()) {
                throw MissingUsernameException("Admin is not in the system. No access Token can be generated ")
            }
            val admin: Admin = optionalAdmin.get()
            if (admin.isDeleted) {
                throw ResourceNotFoundException("Admin has been deleted. No access Token can be generated ")
            }
            id = admin.adminId
            grantedAuthorities =
                admin.permissions.stream().map { permission -> SimpleGrantedAuthority(permission.permission) }
                    .collect(Collectors.toSet())
            for (grantedAuthority: GrantedAuthority in grantedAuthorities) {
                authoritiesAsString += grantedAuthority.authority + " "
            }
        } else {
            val optionalUser: Optional<User> = userRepository.findByUsername(username)
            if (optionalUser.isEmpty) {
                throw MissingUsernameException("User with the provided username does not exist in the system. Unable to generate access token.")
            }
            val user: User = optionalUser.get()
            if (user.isDeleted) {
                throw ResourceNotFoundException("User has been deleted. No access Token can be generated ")
            }
            id = user.id
            grantedAuthorities =
                user.permissions.stream().map { permission -> SimpleGrantedAuthority(permission.permission) }
                    .collect(Collectors.toSet())

            for (grantedAuthority: GrantedAuthority in grantedAuthorities) {
                authoritiesAsString += grantedAuthority.authority + " "
            }

        }

        var expiredTimeOfAccessTokenToLocalDateTime: LocalDateTime? = null
        println("The expire time of access Token is :$expireTimeOfAccessToken")
        if (expireTimeOfAccessToken != null) {
            expiredTimeOfAccessTokenToLocalDateTime = expireTimeOfAccessToken.toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDateTime().plusMinutes(45)
        }

        var newExpiredTimeOfAccessToken = if (expiredTimeOfAccessTokenToLocalDateTime != null) Date.from(
            expiredTimeOfAccessTokenToLocalDateTime.atZone(ZoneId.systemDefault()).toInstant()
        )
        else Date.from(Instant.now().plus(45, ChronoUnit.MINUTES))
        if (expireTimeOfFreshToken != null) {
            if (expireTimeOfFreshToken.before(newExpiredTimeOfAccessToken)) {
                newExpiredTimeOfAccessToken = expireTimeOfFreshToken
                println(
                    "The expire time of access Token is :" + expireTimeOfFreshToken + " this is last access token, which is automatically created using Refresh Token." +
                            "The User must log in again cause the expire time of refresh token will be ended in less than 15 minutes"
                )
            }
        }
        println("The new expire time of access Token is :$newExpiredTimeOfAccessToken")
        val loginResponseDto = LoginResponseDto()
        val accessToken =
            buildToken(isAdmin, id, username, authoritiesAsString, Date(), newExpiredTimeOfAccessToken, null)
        // refresh Token 7 Days
        println("The issue time of refresh Token is :$iat")
        println("The updated refresh Token has a new expire time of access token :$newExpiredTimeOfAccessToken")
        val updatedRefreshToken =

            buildToken(
                isAdmin,
                id,
                username,
                authoritiesAsString,
                Date(),
                newExpiredTimeOfAccessToken,
                expireTimeOfFreshToken
            )

        loginResponseDto.jwtAccessToken = accessToken
        loginResponseDto.jwtRefreshToken = updatedRefreshToken
        return loginResponseDto
    }

    fun getGrantedAuthoritiesAsString(grantedAuthorities: Collection<out GrantedAuthority>): String {
        var grantedAuthoritiesAsString = ""
        for (grantedAuthority in grantedAuthorities) {
            grantedAuthoritiesAsString += grantedAuthority.authority + " "
        }
        return grantedAuthoritiesAsString;
    }
}