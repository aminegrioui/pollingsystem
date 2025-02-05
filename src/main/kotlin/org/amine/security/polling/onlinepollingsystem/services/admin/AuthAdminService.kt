package org.amine.security.polling.onlinepollingsystem.services.admin

import com.google.common.base.Strings
import jakarta.servlet.http.HttpServletRequest
import org.amine.security.polling.onlinepollingsystem.dtos.admin.request.AdminRegisterDto
import org.amine.security.polling.onlinepollingsystem.dtos.admin.response.AdminResponseOfRegistrationDto
import org.amine.security.polling.onlinepollingsystem.dtos.user.request.UserLoginDto
import org.amine.security.polling.onlinepollingsystem.dtos.user.response.LoginResponseDto
import org.amine.security.polling.onlinepollingsystem.exceptions.*
import org.amine.security.polling.onlinepollingsystem.models.admin.Admin
import org.amine.security.polling.onlinepollingsystem.models.permission.Permission
import org.amine.security.polling.onlinepollingsystem.repos.admin.AdminRepository
import org.amine.security.polling.onlinepollingsystem.repos.permissions.PermissionRepository
import org.amine.security.polling.onlinepollingsystem.system.audit.admin.service.AdminAuditService
import org.amine.security.polling.onlinepollingsystem.system.historic.admin.services.AdminHistoricAuthService
import org.amine.security.polling.onlinepollingsystem.system.security.jwtgen.JwtGenerator
import org.amine.security.polling.onlinepollingsystem.system.security.services.AuthStorage
import org.amine.security.polling.onlinepollingsystem.system.tools.AppTool
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.InternalAuthenticationServiceException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.*

@Service
class AuthAdminService(
    private var appTool: AppTool,
    private var adminRepository: AdminRepository,
    private val passwordEncoder: PasswordEncoder,
    private val permissionRepository: PermissionRepository,
    val authenticationManger: AuthenticationManager,
    val jwtGenerator: JwtGenerator,
    val historicAuthService: AdminHistoricAuthService,
    val authStorage: AuthStorage,
    val adminAuditService: AdminAuditService,
) {

    fun registerAdmin(
        adminRegisterDto: AdminRegisterDto, request: HttpServletRequest?
    ): AdminResponseOfRegistrationDto {
        val adminResponseOfRegistrationDto = AdminResponseOfRegistrationDto();

        if (Strings.isNullOrEmpty(adminRegisterDto.username) || Strings.isNullOrEmpty(adminRegisterDto.password) || Strings.isNullOrEmpty(
                adminRegisterDto.email
            )
        ) {
            throw ValidationDataException("To Register a new admin you have to give username, email and password ");
        }
        if (!appTool.validateUsername(adminRegisterDto.username)) {
            throw ValidationDataException("The given username is not valid. It must have at minimum 8 aplha character !!");
        }
        if (!appTool.validatePassword(adminRegisterDto.password)) {
            throw ValidationDataException("The given password is not valid. It must have at minimum 10 alphanumeric  !!")
        }
        if (!appTool.checkValidationOfGivenEmail(adminRegisterDto.email)) {
            throw EmailValidationException("This email is not valid: " + adminRegisterDto.email)
        }
        val optionalAdminUsingUsername: Optional<Admin> = adminRepository.findByUsername(adminRegisterDto.username);
        val optionalAdminUsingEmail: Optional<Admin> = adminRepository.findByEmail(adminRegisterDto.email);

        if (optionalAdminUsingUsername.isEmpty && optionalAdminUsingEmail.isEmpty) {
            val admin = Admin();
            admin.username = adminRegisterDto.username
            // send email to validate the activation of email
            admin.email = adminRegisterDto.email
            admin.password = passwordEncoder.encode(adminRegisterDto.password)
            admin.isCredentialsNonExpired = true
            admin.isEnabled = true
            admin.isAccountNonExpired = true
            admin.isAccountNonLocked = true
            val initialUserPermission: MutableSet<Permission> = HashSet()
            if (adminRegisterDto.adminId != null) {
                initialUserPermission.add(permissionRepository.findByPermission("ROLE_ADMIN").get())
                admin.parentIdOfAdmin = adminRegisterDto.adminId
            } else {
                val permissions: MutableSet<String> =
                    mutableSetOf("ROLE_ADMIN", "WRITE_USER",  "WRITE_POLE", "WRITE_ADMIN");
                var optionalPermission: Optional<Permission>
                for (permission in permissions) {
                    optionalPermission = permissionRepository.findByPermission(permission)
                    if (optionalPermission.isPresent) {
                        initialUserPermission.add(optionalPermission.get())
                    }
                }
            }
            admin.permissions = initialUserPermission
            adminRepository.save(admin);
            adminResponseOfRegistrationDto.userId = admin.adminId
            adminResponseOfRegistrationDto.username = admin.username
            adminResponseOfRegistrationDto.description = "Admin with userName " + admin.username + " was created"
            // audit
            if (request != null) {
                val ipAddress = appTool.getUerIpAddress(request)
                adminAuditService.saveAction(
                    "LOGIN_ADMIN", adminRegisterDto.username, ipAddress, "", appTool.getNowTime(), null
                )
            }

            return adminResponseOfRegistrationDto;
        }
        throw AlreadyExistUserException(("Admin with this userName " + adminRegisterDto.username) + " and email: " + adminRegisterDto.email + " was already existed ")
    }

    fun loginAdmin(loginDto: UserLoginDto, request: HttpServletRequest): LoginResponseDto {
        if (Strings.isNullOrEmpty(loginDto.username) || Strings.isNullOrEmpty(loginDto.password)) {
            throw ValidationDataException("To login you have to give username and password ")
        }
        val authAdminHistoric = historicAuthService.traceAuthHistoric(loginDto.username, "LOGIN")
        try {
            val usernamePasswordAuthenticationToken =
                UsernamePasswordAuthenticationToken("admin_${loginDto.username}", loginDto.password)
            val authentication: Authentication = authenticationManger.authenticate(usernamePasswordAuthenticationToken)
            val jwtToken = jwtGenerator.generateAccessAndRefreshToken(authentication)
            authAdminHistoric.isSuccessOperation = true
            historicAuthService.saveAuthHistoric(authAdminHistoric)
            authStorage.addAuthentication(authentication, true)

            // audit
            val ipAddress = appTool.getUerIpAddress(request)
            adminAuditService.saveAction(
                "LOGIN_ADMIN", loginDto.username, ipAddress, "", appTool.getNowTime(), jwtToken.tokenExpireTime
            )
            return jwtToken
        } catch (ex: InternalAuthenticationServiceException) {
            throw UserNameNotFoundException("User with this username is not found")
        } catch (ex: AuthenticationException) {
            historicAuthService.checkAdminAccountLocked(loginDto.username, "LOGIN")
            historicAuthService.saveAuthHistoric(authAdminHistoric)
            throw BadCredentialsException("Password incorrect ")
        } catch (ex: UserLockoutException) {
            throw UserLockoutException(ex.message)
        } catch (ex: Exception) {
            throw GlobalException(ex.message)
        }
    }
}