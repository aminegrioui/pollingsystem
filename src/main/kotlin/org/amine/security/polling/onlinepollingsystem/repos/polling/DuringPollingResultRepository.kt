package org.amine.security.polling.onlinepollingsystem.repos.polling;

import org.amine.security.polling.onlinepollingsystem.models.polling.DuringPollingResult
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface DuringPollingResultRepository : JpaRepository<DuringPollingResult, Long> {
    fun findDuringPollingResultsByUserIdAndPollId(userId: Long, pollId: Long): Optional<DuringPollingResult>
}