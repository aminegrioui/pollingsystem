package org.amine.security.polling.onlinepollingsystem.services.admin

import com.google.common.base.Strings
import jakarta.transaction.Transactional
import org.amine.security.polling.onlinepollingsystem.dtos.admin.request.AdminRegisterDto
import org.amine.security.polling.onlinepollingsystem.dtos.admin.request.PermissionDTO
import org.amine.security.polling.onlinepollingsystem.dtos.admin.response.AdminResponseOfRegistrationDto
import org.amine.security.polling.onlinepollingsystem.dtos.admin.response.SmallAdminResponseDto
import org.amine.security.polling.onlinepollingsystem.dtos.poll.request.ControlPollDto
import org.amine.security.polling.onlinepollingsystem.dtos.user.request.UserRegisterDto
import org.amine.security.polling.onlinepollingsystem.dtos.user.response.SmallUserDto
import org.amine.security.polling.onlinepollingsystem.dtos.user.response.UserResponseOfRegistrationDto
import org.amine.security.polling.onlinepollingsystem.exceptions.AlreadyExistUserException
import org.amine.security.polling.onlinepollingsystem.exceptions.EmailValidationException
import org.amine.security.polling.onlinepollingsystem.exceptions.ResourceNotFoundException
import org.amine.security.polling.onlinepollingsystem.exceptions.ValidationDataException
import org.amine.security.polling.onlinepollingsystem.models.admin.Admin
import org.amine.security.polling.onlinepollingsystem.repos.admin.AdminRepository
import org.amine.security.polling.onlinepollingsystem.repos.permissions.PermissionRepository
import org.amine.security.polling.onlinepollingsystem.repos.user.UserRepository
import org.amine.security.polling.onlinepollingsystem.services.polling.PollingService
import org.amine.security.polling.onlinepollingsystem.services.user.AuthUserService
import org.amine.security.polling.onlinepollingsystem.services.user.UserService
import org.amine.security.polling.onlinepollingsystem.system.audit.admin.service.AdminAuditService
import org.amine.security.polling.onlinepollingsystem.system.blacklist.admin.services.AdminBlackListService
import org.amine.security.polling.onlinepollingsystem.system.blacklist.user.repositories.UserBlackListRepository
import org.amine.security.polling.onlinepollingsystem.system.blacklist.user.services.BlackListService
import org.amine.security.polling.onlinepollingsystem.system.security.jwt.JwtTool
import org.amine.security.polling.onlinepollingsystem.system.security.jwt.ParsTokenResponse
import org.amine.security.polling.onlinepollingsystem.system.security.services.AuthenticationService
import org.amine.security.polling.onlinepollingsystem.system.tools.AppTool
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service


@Service
class AdminService(
    private val jwtTool: JwtTool,
    private val appTool: AppTool,
    private val adminBlackListService: AdminBlackListService,
    private val permissionRepository: PermissionRepository,
    private val userRepository: UserRepository,
    private val blackListService: BlackListService,
    private val userBlackListRepository: UserBlackListRepository,
    private val authenticationService: AuthenticationService,
    private val adminAuditService: AdminAuditService,
    private val adminAuthAdminService: AuthAdminService,
    private val userService: UserService,
    private val authUserService: AuthUserService,
    private val adminRepository: AdminRepository,
    private val pollingService: PollingService,
) {

    @Transactional
    fun registerNewAdmin(
        adminRegisterDto: AdminRegisterDto,
        requestHeader: HttpHeaders,
    ): AdminResponseOfRegistrationDto {
        val parseToken: ParsTokenResponse = jwtTool.parseTokenResponse
        checkTokenOrUsernameInBlackList(parseToken.username, requestHeader)
        adminRegisterDto.adminId = parseToken.id
        val adminResponseOfRegistrationDto = adminAuthAdminService.registerAdmin(adminRegisterDto, null)
        // audit
        adminAuditService.saveAction(
            "REGISTER_NEW_ADMIN",
            parseToken.username,
            appTool.getUerIpAddress(requestHeader),
            "",
            appTool.getNowTime(),
            null
        )
        return adminResponseOfRegistrationDto
    }

    @Transactional
    fun showAllAdmins(requestHeader: HttpHeaders): List<SmallAdminResponseDto> {
        val parseToken: ParsTokenResponse = jwtTool.parseTokenResponse
        checkTokenOrUsernameInBlackList(parseToken.username, requestHeader)
        val smallAdmins: List<SmallAdminResponseDto> =
            adminRepository.findAll().stream().map { convertAdminTo(it) }.toList()
        return smallAdmins
    }

    fun findAdminById(id: Long?, requestHeader: HttpHeaders): SmallAdminResponseDto {
        val parseToken: ParsTokenResponse = jwtTool.parseTokenResponse
        checkTokenOrUsernameInBlackList(parseToken.username, requestHeader)
        if (id == null) {
            throw ValidationDataException("Admin must have a value")
        }
        val optionalAdmin = adminRepository.findById(id)
        if (optionalAdmin.isEmpty) {
            throw ResourceNotFoundException("Admin not found with id $id")
        }
        return convertAdminTo(optionalAdmin.get())
    }

    @Transactional
    fun deleteAdminById(adminId: Long, requestHeader: HttpHeaders): Boolean {
        val parseToken: ParsTokenResponse = jwtTool.parseTokenResponse
        checkTokenOrUsernameInBlackList(parseToken.username, requestHeader)
        val optional = adminRepository.findById(adminId)
        if (optional.isEmpty) {
            throw ResourceNotFoundException("Admin not found with id $adminId")
        }
        val admin = optional.get()
        if (admin.isDeleted) {
            throw ValidationDataException("User already deleted with id $adminId")
        }
        admin.isDeleted = true
        adminRepository.save(admin)

        // audit
        adminAuditService.saveAction(
            "DELETE_ADMIN",
            parseToken.username,
            appTool.getUerIpAddress(requestHeader),
            "",
            appTool.getNowTime(),
            null
        )

        return true
    }

    @Transactional
    fun registerNewUser(userDto: UserRegisterDto, requestHeader: HttpHeaders): UserResponseOfRegistrationDto {
        val parseToken: ParsTokenResponse = jwtTool.parseTokenResponse
        checkTokenOrUsernameInBlackList(parseToken.username, requestHeader)
        val userResponseOfRegistrationDto = authUserService.registerUser(userDto, null)
        // audit
        adminAuditService.saveAction(
            "REGISTER_NEW_USER",
            parseToken.username,
            appTool.getUerIpAddress(requestHeader),
            "",
            appTool.getNowTime(),
            null
        )
        return userResponseOfRegistrationDto
    }

    fun getAllUsers(requestHeader: HttpHeaders): MutableList<SmallUserDto> {
        val parseToken: ParsTokenResponse = jwtTool.parseTokenResponse
        checkTokenOrUsernameInBlackList(parseToken.username, requestHeader)
        return userService.findAllUsers()
    }

    fun findUserById(id: Long?, requestHeader: HttpHeaders): SmallUserDto {
        val parseToken: ParsTokenResponse = jwtTool.parseTokenResponse
        checkTokenOrUsernameInBlackList(parseToken.username, requestHeader)
        if (id == null) {
            throw ValidationDataException("User must have a value")
        }
        return userService.findUserById(id)
    }

    @Transactional
    fun deleteUserById(userId: Long, requestHeader: HttpHeaders): Boolean {
        val parseToken: ParsTokenResponse = jwtTool.parseTokenResponse
        checkTokenOrUsernameInBlackList(parseToken.username, requestHeader)
        val isDeleted = userService.deleteUserById(userId)
        // audit
        adminAuditService.saveAction(
            "DELETE_USER_BY_ID",
            parseToken.username,
            appTool.getUerIpAddress(requestHeader),
            "",
            appTool.getNowTime(),
            null
        )
        return isDeleted
    }

    // Control Poll

    fun finishPoll(controlPollDto: ControlPollDto, requestHeader: HttpHeaders): String {
        val parseToken: ParsTokenResponse = jwtTool.parseTokenResponse
        checkTokenOrUsernameInBlackList(parseToken.username, requestHeader)
        controlPollDto.isAdmin = true
        return pollingService.finishPoll(controlPollDto, requestHeader)
    }

    fun openPoll(controlPollDto: ControlPollDto, requestHeader: HttpHeaders): String {
        val parseToken: ParsTokenResponse = jwtTool.parseTokenResponse
        checkTokenOrUsernameInBlackList(parseToken.username, requestHeader)
        controlPollDto.isAdmin = true
        return pollingService.openPoll(controlPollDto, requestHeader)
    }

    fun cancelPoll(controlPollDto: ControlPollDto, requestHeader: HttpHeaders): String {
        val parseToken: ParsTokenResponse = jwtTool.parseTokenResponse
        checkTokenOrUsernameInBlackList(parseToken.username, requestHeader)
        controlPollDto.isAdmin = true
        return pollingService.cancelPoll(controlPollDto, requestHeader)
    }

    fun pendingPoll(controlPollDto: ControlPollDto, requestHeader: HttpHeaders): String {
        val parseToken: ParsTokenResponse = jwtTool.parseTokenResponse
        checkTokenOrUsernameInBlackList(parseToken.username, requestHeader)
        controlPollDto.isAdmin = true
        return pollingService.pendingPoll(controlPollDto, requestHeader)
    }

    fun deletePollById(controlPollDto: ControlPollDto, requestHeader: HttpHeaders): Boolean {
        val parseToken: ParsTokenResponse = jwtTool.parseTokenResponse
        checkTokenOrUsernameInBlackList(parseToken.username, requestHeader)
        controlPollDto.isAdmin = true
        return pollingService.deletePollById(controlPollDto, requestHeader)
    }

    fun togglePermission(userPermissionDTO: PermissionDTO, requestHeader: HttpHeaders): String {
        val parseToken: ParsTokenResponse = jwtTool.parseTokenResponse
        checkTokenOrUsernameInBlackList(parseToken.username, requestHeader)
        if (Strings.isNullOrEmpty(userPermissionDTO.permission)) {
            throw ValidationDataException("You have to give a permission")
        }
        if (!appTool.setOfUserPermissions().contains(userPermissionDTO.permission)) {
            throw ValidationDataException("Permission must be one of ${userPermissionDTO.permission}")
        }
        if (userPermissionDTO.id == null) {
            throw ValidationDataException("You have to give a userId")
        }
        if (userPermissionDTO.isAdmin) {
            return toggleAdminPermission(userPermissionDTO, appTool.getUerIpAddress(requestHeader))
        }

        return toggleUserPermission(userPermissionDTO, appTool.getUerIpAddress(requestHeader))
    }

    private fun toggleUserPermission(userPermissionDTO: PermissionDTO, ipAddress: String): String {
        val optionalUser = userRepository.findById(userPermissionDTO.id!!)
        if (optionalUser.isEmpty) {
            throw ResourceNotFoundException("User with ${userPermissionDTO.id} not found")
        }
        val user = optionalUser.get()
        if (user.isDeleted) {
            throw ResourceNotFoundException("User with ${userPermissionDTO.id} is deleted")
        }
        val optionalPermission = permissionRepository.findByPermission(userPermissionDTO.permission)
        val permission = optionalPermission.get()
        if (userPermissionDTO.permissionEnabled) {
            if (user.permissions.contains(permission)) {
                throw ValidationDataException(
                    "This permission ${userPermissionDTO.permission} is already  belong to the ${
                        user.permissions.stream().toList()
                    }"
                )
            }
            user.permissions.add(permission)
            userRepository.save(user)
            // Audit
            auditAction(false, user.username, ipAddress)
            return "The user has a new permission: ${permission.permission}. You have to log out and log in again to get this permission"
        }
        if (!user.permissions.contains(permission)) {
            throw ValidationDataException(
                "This permission ${userPermissionDTO.permission} is already not belong to the ${
                    user.permissions.stream().toList()
                }"
            )
        }
        user.permissions.remove(permission)
        userRepository.save(user)
        // Audit
        auditAction(true, user.username, ipAddress)

        // This in Kafka
        if (userPermissionDTO.logOutUser) {
            if (isLogOutUser(userPermissionDTO.permission, userPermissionDTO.timeOfExpireTimeInMinutes))
                return "The user has no more this permission: ${permission.permission}. You have to wait ${userPermissionDTO.timeOfExpireTimeInMinutes} to use Apis. Please log in again"
        }
        return "The user has no more this permission: ${permission.permission}"
    }

    private fun toggleAdminPermission(userPermissionDTO: PermissionDTO, ipAddress: String): String {
        val optionalAdmin = adminRepository.findById(userPermissionDTO.id!!)
        if (optionalAdmin.isEmpty) {
            throw ResourceNotFoundException("Admin with ${userPermissionDTO.id} not found")
        }
        val admin = optionalAdmin.get()
        if (admin.isDeleted) {
            throw ResourceNotFoundException("Admin with ${userPermissionDTO.id} is deleted")
        }
        val optionalPermission = permissionRepository.findByPermission(userPermissionDTO.permission)
        val permission = optionalPermission.get()
        if (userPermissionDTO.permissionEnabled) {
            if (admin.permissions.contains(permission)) {
                throw ValidationDataException(
                    "This permission ${userPermissionDTO.permission} is already  belong to the ${
                        admin.permissions.stream().toList()
                    }"
                )
            }
            admin.permissions.add(permission)
            adminRepository.save(admin)
            // Audit
            auditAction(false, admin.username, ipAddress)
            return "The Admin has a new permission: ${permission.permission}. You have to log out and log in again to get this permission"
        }
        if (!admin.permissions.contains(permission)) {
            throw ValidationDataException(
                "This permission ${userPermissionDTO.permission} is already not belong to the ${
                    admin.permissions.stream().toList()
                }"
            )
        }
        admin.permissions.remove(permission)
        adminRepository.save(admin)
        // Audit
        auditAction(true, admin.username, ipAddress)

        // This in Kafka
        if (userPermissionDTO.logOutUser) {
            if (isLogOutUser(userPermissionDTO.permission, userPermissionDTO.timeOfExpireTimeInMinutes))
                return "The Admin has no more this permission: ${permission.permission}. You have to wait ${userPermissionDTO.timeOfExpireTimeInMinutes} to use Apis. Please log in again"
        }
        return "The Admin has no more this permission: ${permission.permission}"
    }

    private fun convertAdminTo(admin: Admin): SmallAdminResponseDto {
        val smallAdminResponseDto = SmallAdminResponseDto()
        smallAdminResponseDto.adminId = admin.adminId
        smallAdminResponseDto.username = admin.username
        smallAdminResponseDto.email = admin.email
        if (admin.users.isNotEmpty()) {
            smallAdminResponseDto.users = admin.users.stream().map { it.username }.toList()
        }
        smallAdminResponseDto.permissions = admin.permissions.stream().map { it.permission }.toList()

        return smallAdminResponseDto
    }

    private fun isLogOutUser(username: String, timeOfExpireTimeInMinutes: Long): Boolean {
        if (authenticationService.isAuthenticated(username)) {
            val optionalBlackListEntry = userBlackListRepository.findByUsername(username)
            if (optionalBlackListEntry.isEmpty) {
                blackListService.saveBlackListEntry(
                    username,
                    "",
                    "DELETE_PERMISSION_ROLE",
                    timeOfExpireTimeInMinutes
                )
                return true
            }
        }
        return false
    }

    private fun auditAction(isDeleted: Boolean, username: String, ipAddress: String) {
        val action = if (isDeleted) "DELETE_PERMISSION_ROLE" else "ADD_PERMISSION_ROLE"
        adminAuditService.saveAction(
            action,
            username,
            ipAddress,
            "",
            appTool.getNowTime(),
            null
        )
    }

    private fun checkTokenOrUsernameInBlackList(username: String?, requestHeader: HttpHeaders) {
        adminBlackListService.checkTokenOrUsernameInBlackList(username, requestHeader, null)
    }


}