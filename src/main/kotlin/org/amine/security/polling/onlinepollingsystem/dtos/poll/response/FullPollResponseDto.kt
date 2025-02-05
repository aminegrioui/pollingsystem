package org.amine.security.polling.onlinepollingsystem.dtos.poll.response

import org.amine.security.polling.onlinepollingsystem.dtos.user.response.SmallUserDto


class FullPollResponseDto {

    var pollId: Long = 0

    var title: String = ""

    var creator: String = ""

    var description: String = ""

    var options: Set<String> = setOf()

    var category: String = ""

    var participated: List<SmallUserDto> = listOf();

    var startPollingTime: String = ""

    var endPollingTime: String = ""

    var visibility: String = ""

    var pollType: String = ""

    var status: String = ""

    var resultState: String = ""

    var isPublicResults: Boolean? = null
}