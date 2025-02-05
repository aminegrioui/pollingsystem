package org.amine.security.polling.onlinepollingsystem.models.participated

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class PollParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
    var poll_id: Long = 0
    var participant_id: Long = 0
    var username: String? = null
}