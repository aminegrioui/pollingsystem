package org.amine.security.polling.onlinepollingsystem.system.audit.user.controller

import org.amine.security.polling.onlinepollingsystem.system.audit.user.service.UserAuditService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/user/audit")
class UserAuditController(val auditService: UserAuditService) {

    @GetMapping("/{numberOfDays}")
    fun findAdminAuditLastTime(@PathVariable numberOfDays: Long): ResponseEntity<Any> {
        return ResponseEntity.status(HttpStatus.OK).body(auditService.findAdminAuditLastTime(numberOfDays))
    }
}