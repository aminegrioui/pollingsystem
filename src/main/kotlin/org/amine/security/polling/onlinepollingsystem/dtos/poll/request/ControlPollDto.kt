package org.amine.security.polling.onlinepollingsystem.dtos.poll.request

class ControlPollDto {
    var pollingId: Long? = null
    var isAdmin: Boolean = false
    var reason: String = ""
    var startPollingTime: String = ""
    var endPollingTime: String = ""
}