package org.amine.security.polling.onlinepollingsystem.services.user

import com.google.common.base.Strings
import jakarta.servlet.http.HttpServletRequest
import org.amine.security.polling.onlinepollingsystem.dtos.user.request.UserLoginDto
import org.amine.security.polling.onlinepollingsystem.dtos.user.request.UserRegisterDto
import org.amine.security.polling.onlinepollingsystem.dtos.user.response.LoginResponseDto
import org.amine.security.polling.onlinepollingsystem.dtos.user.response.UserResponseOfRegistrationDto
import org.amine.security.polling.onlinepollingsystem.exceptions.*
import org.amine.security.polling.onlinepollingsystem.models.permission.Permission
import org.amine.security.polling.onlinepollingsystem.models.users.User
import org.amine.security.polling.onlinepollingsystem.repos.admin.AdminRepository
import org.amine.security.polling.onlinepollingsystem.repos.permissions.PermissionRepository
import org.amine.security.polling.onlinepollingsystem.repos.user.UserRepository
import org.amine.security.polling.onlinepollingsystem.system.audit.user.service.UserAuditService
import org.amine.security.polling.onlinepollingsystem.system.historic.user.services.HistoricAuthService
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
import java.util.logging.Logger

@Service
class AuthUserService(
    val appTool: AppTool,
    var userRepository: UserRepository,
    val permissionRepository: PermissionRepository,
    val passwordEncoder: PasswordEncoder,
    val authenticationManger: AuthenticationManager,
    val jwtGenerator: JwtGenerator,
    val historicAuthService: HistoricAuthService,
    val authStorage: AuthStorage,
    val auditService: UserAuditService,
    private val adminRepository: AdminRepository
) {
    val logger = Logger.getLogger(AuthUserService::class.java.name)
    fun registerUser(userDto: UserRegisterDto, request: HttpServletRequest?): UserResponseOfRegistrationDto {
        val userResponseOfRegistrationDto = UserResponseOfRegistrationDto();

        if (Strings.isNullOrEmpty(userDto.username) ||
            Strings.isNullOrEmpty(userDto.password) ||
            Strings.isNullOrEmpty(userDto.email)
        ) {
            throw ValidationDataException("To Register a new User you have to give username, email and password ");
        }
        if (!appTool.validateUsername(userDto.username)) {
            throw ValidationDataException("The given username is not valid. It must have at minimum 8 aplha character !!");
        }
        if (!appTool.validatePassword(userDto.password)) {
            throw ValidationDataException("The given password is not valid. It must have at minimum 10 alphanumeric  !!")
        }
        if (!appTool.checkValidationOfGivenEmail(userDto.email)) {
            throw EmailValidationException("This email is not valid: " + userDto.email)
        }
        val optionalUserUsingUsername: Optional<User> = userRepository.findByUsername(userDto.username);
        val optionalUserUsingEmail: Optional<User> = userRepository.findByEmail(userDto.email);

        if (optionalUserUsingUsername.isEmpty && optionalUserUsingEmail.isEmpty) {
            val user = User();
            user.username = userDto.username
            // send email to validate the activation of email
            user.email = userDto.email
            user.password = passwordEncoder.encode(userDto.password)
            user.isCredentialsNonExpired = true
            user.isEnabled = true
            user.isAccountNonExpired = true
            user.isAccountNonLocked = true
            val initialUserPermission: MutableSet<Permission> = HashSet();
            val optionalPermission = permissionRepository.findByPermission("ROLE_USER")
            if (optionalPermission.isEmpty) {
                throw ResourceNotFoundException("Permission not found")
            }
            initialUserPermission.add(optionalPermission.get())
            user.permissions = initialUserPermission
            if (userDto.adminId != null) {
                val optionalAdmin = adminRepository.findById(userDto.adminId!!)
                if (optionalAdmin.isEmpty) {
                    throw ResourceNotFoundException("Admin not found with this admin Id:  ${userDto.adminId}")
                }
                val admin = optionalAdmin.get();
                if (admin.isDeleted) {
                    throw ValidationDataException("This admin  with id: ${admin.adminId} is deleted");
                }
                admin.users.add(user)
            }
            userRepository.save(user)
            userResponseOfRegistrationDto.userId = user.id
            userResponseOfRegistrationDto.username = userDto.username
            userResponseOfRegistrationDto.description = "User with userName " + userDto.username + " was created"
            // audit
            if (request != null) {
                val ipAddress = appTool.getUerIpAddress(request)
                auditService.saveAction("REGISTER_USER", user.username, ipAddress, "", appTool.getNowTime(), null)
            }
            return userResponseOfRegistrationDto;

        }
        throw AlreadyExistUserException(("User with this userName " + userDto.username) + " and email: " + userDto.email + " was already existed ")
    }

    fun loginUser(loginDto: UserLoginDto, request: HttpServletRequest): LoginResponseDto {
        if (Strings.isNullOrEmpty(loginDto.username) || Strings.isNullOrEmpty(loginDto.password)) {
            throw ValidationDataException("To login you have to give username and password ")
        }
        val authUserHistoric = historicAuthService.traceAuthHistoric(loginDto.username, "LOGIN")
        try {
            val usernamePasswordAuthenticationToken =
                UsernamePasswordAuthenticationToken(loginDto.username, loginDto.password)
            val authentication: Authentication = authenticationManger.authenticate(usernamePasswordAuthenticationToken)
            val jwtToken = jwtGenerator.generateAccessAndRefreshToken(authentication)
            authUserHistoric.isSuccessOperation = true
            historicAuthService.saveAuthHistoric(authUserHistoric)
            authStorage.addAuthentication(authentication, false)
            // audit
            val ipAddress = appTool.getUerIpAddress(request)
            auditService.saveAction(
                "LOGIN_USER",
                loginDto.username,
                ipAddress,
                "",
                appTool.getNowTime(),
                jwtToken.tokenExpireTime
            )
            return jwtToken
        } catch (ex: InternalAuthenticationServiceException) {
            throw UserNameNotFoundException("User with this username is not found")
        } catch (ex: AuthenticationException) {
            historicAuthService.checkUserAccountLocked(loginDto.username, "LOGIN")
            historicAuthService.saveAuthHistoric(authUserHistoric)
            throw BadCredentialsException("Password incorrect ")
        } catch (ex: UserLockoutException) {
            throw UserLockoutException(ex.message)
        } catch (ex: Exception) {
            throw GlobalException(ex.message)
        }
    }
}