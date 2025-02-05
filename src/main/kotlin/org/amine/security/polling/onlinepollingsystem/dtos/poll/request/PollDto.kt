package org.amine.security.polling.onlinepollingsystem.dtos.poll.request


import org.amine.security.polling.onlinepollingsystem.enumuration.polling.PollType

class PollDto {
    var title: String = ""

    var description: String = ""

    var options: Set<String> = setOf()

    var category: String = ""

    var participated: Set<Long>? = null;

    var startPollingTime: String = ""

    var endPollingTime: String = ""

    var pollType: String? = PollType.SINGLE_CHOICE.name

    var isPublicResults: Boolean? = null
}