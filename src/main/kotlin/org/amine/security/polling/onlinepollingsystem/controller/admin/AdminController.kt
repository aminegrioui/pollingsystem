package org.amine.security.polling.onlinepollingsystem.controller.admin

import org.amine.security.polling.onlinepollingsystem.dtos.admin.request.AdminRegisterDto
import org.amine.security.polling.onlinepollingsystem.dtos.admin.request.PermissionDTO
import org.amine.security.polling.onlinepollingsystem.dtos.admin.response.AdminResponseOfRegistrationDto
import org.amine.security.polling.onlinepollingsystem.dtos.admin.response.SmallAdminResponseDto
import org.amine.security.polling.onlinepollingsystem.dtos.poll.request.ControlPollDto
import org.amine.security.polling.onlinepollingsystem.dtos.user.request.UserRegisterDto
import org.amine.security.polling.onlinepollingsystem.dtos.user.response.SmallUserDto
import org.amine.security.polling.onlinepollingsystem.dtos.user.response.UserResponseOfRegistrationDto
import org.amine.security.polling.onlinepollingsystem.services.admin.AdminService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/polling/v1/admins")
class AdminController(val adminService: AdminService) {
    @PostMapping("/admin")
    fun createAdmin(
        @RequestBody adminRegisterDto: AdminRegisterDto,
        @RequestHeader requestHeader: HttpHeaders
    ): ResponseEntity<AdminResponseOfRegistrationDto> {
                return ResponseEntity.status(HttpStatus.CREATED)
            .body(adminService.registerNewAdmin(adminRegisterDto, requestHeader))
    }

    @GetMapping("/allAdmins")
    fun getAdmins(@RequestHeader requestHeader: HttpHeaders): ResponseEntity<List<SmallAdminResponseDto>> {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.showAllAdmins(requestHeader))
    }

    @GetMapping("/admin/{id}")
    fun findAdminById(@PathVariable id: Long, requestHeader: HttpHeaders): ResponseEntity<SmallAdminResponseDto> {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.findAdminById(id, requestHeader))
    }

    @DeleteMapping("/admin/{adminId}")
    fun removeAdminById(
        @PathVariable adminId: Long,
        @RequestHeader requestHeader: HttpHeaders
    ): ResponseEntity<Boolean> {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.deleteAdminById(adminId, requestHeader))
    }

    @PutMapping("/permissions")
    fun togglePermission(
        @RequestBody userPermissionDTO: PermissionDTO, @RequestHeader requestHeader: HttpHeaders
    ): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(adminService.togglePermission(userPermissionDTO, requestHeader))
    }


    @PostMapping("/user")
    fun createUser(
        @RequestBody userRegisterDto: UserRegisterDto,
        @RequestHeader requestHeader: HttpHeaders
    ): ResponseEntity<UserResponseOfRegistrationDto> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(adminService.registerNewUser(userRegisterDto, requestHeader))
    }

    @GetMapping("/allUsers")
    fun getAllUsers(@RequestHeader requestHeader: HttpHeaders): ResponseEntity<List<SmallUserDto>> {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.getAllUsers(requestHeader))
    }

    @GetMapping("user/{userId}")
    fun getUserById(
        @PathVariable userId: Long,
        @RequestHeader requestHeader: HttpHeaders
    ): ResponseEntity<SmallUserDto> {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.findUserById(userId, requestHeader))
    }

    @DeleteMapping("/user/{userId}")
    fun removeUserById(
        @PathVariable userId: Long,
        @RequestHeader requestHeader: HttpHeaders
    ): ResponseEntity<Boolean> {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.deleteUserById(userId, requestHeader))
    }

    @PutMapping("/poll/finish")
    fun finishPoll(@RequestBody controlPollDto: ControlPollDto, requestHeader: HttpHeaders): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.finishPoll(controlPollDto, requestHeader))
    }

    @PutMapping("/poll/open")
    fun openPoll(@RequestBody controlPollDto: ControlPollDto, requestHeader: HttpHeaders): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.openPoll(controlPollDto, requestHeader))
    }

    @PutMapping("/poll/cancel")
    fun cancelPoll(@RequestBody controlPollDto: ControlPollDto, requestHeader: HttpHeaders): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.cancelPoll(controlPollDto, requestHeader))
    }

    @PutMapping("/poll/pending")
    fun pendingPoll(@RequestBody controlPollDto: ControlPollDto, requestHeader: HttpHeaders): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.pendingPoll(controlPollDto, requestHeader))
    }

    @DeleteMapping("/poll/delete")
    fun deletePoll(@RequestBody controlPollDto: ControlPollDto, requestHeader: HttpHeaders): ResponseEntity<Boolean> {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.deletePollById(controlPollDto, requestHeader))
    }

}