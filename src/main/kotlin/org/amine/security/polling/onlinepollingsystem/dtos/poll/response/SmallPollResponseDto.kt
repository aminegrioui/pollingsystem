package org.amine.security.polling.onlinepollingsystem.dtos.poll.response


class SmallPollResponseDto {

    var pollId: Long = 0

    var title: String = ""

    var creator: String = ""

    var description: String = ""

    var options: Set<String> = setOf()

    var status: String = ""
}