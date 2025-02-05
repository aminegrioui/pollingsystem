package org.amine.security.polling.onlinepollingsystem.dtos.poll.request

class ParticipatePollDto {
    var pollId: Long = 0

    var selectedOptions: Set<String> = setOf()
}