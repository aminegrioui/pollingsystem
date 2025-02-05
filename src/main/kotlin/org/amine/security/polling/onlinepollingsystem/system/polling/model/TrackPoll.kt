package org.amine.security.polling.onlinepollingsystem.system.polling.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import lombok.ToString
import java.time.ZonedDateTime

@Entity
@ToString
class TrackPoll {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0
    var userId: Long = 0
    var numberOfTry: Int = 0
    var startTimeOfRemoveParticipateRight: ZonedDateTime? = null
    var endTimeOfRemoveParticipateRight: ZonedDateTime? = null
}