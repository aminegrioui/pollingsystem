package org.amine.security.polling.onlinepollingsystem.repos.polling;

import org.amine.security.polling.onlinepollingsystem.models.polling.Poll
import org.springframework.data.jpa.repository.JpaRepository

interface PollRepository : JpaRepository<Poll, Long> {
}