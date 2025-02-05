package org.amine.security.polling.onlinepollingsystem.system.audit.admin.controller

import org.amine.security.polling.onlinepollingsystem.system.audit.admin.service.AdminAuditService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/audit")
class AdminAuditController(private val auditService: AdminAuditService) {

    @GetMapping("/{numberOfDays}")
    fun findAdminAuditLastTime(@PathVariable numberOfDays: Long): ResponseEntity<Any> {
        return ResponseEntity.status(HttpStatus.OK).body(auditService.findAdminAuditLastTime(numberOfDays))
    }
}