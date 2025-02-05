package org.amine.security.polling.onlinepollingsystem.system.polling.repo

import org.amine.security.polling.onlinepollingsystem.system.polling.model.TrackPoll
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface TrackPollRepository : JpaRepository<TrackPoll, Long> {
    fun findByUserId(userId: Long): Optional<TrackPoll>
}