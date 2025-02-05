package org.amine.security.polling.onlinepollingsystem.repos.participant

import org.amine.security.polling.onlinepollingsystem.models.participated.PollParticipant
import org.springframework.data.jpa.repository.JpaRepository

interface PollParticipantRepository: JpaRepository<PollParticipant,Long> {
}