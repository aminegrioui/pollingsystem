package org.amine.security.polling.onlinepollingsystem.repos.polling;

import org.amine.security.polling.onlinepollingsystem.models.polling.PollingResult
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PollingResultRepository : JpaRepository<PollingResult, Long> {
    fun findByPollId(pollId: Long): Optional<PollingResult>
}