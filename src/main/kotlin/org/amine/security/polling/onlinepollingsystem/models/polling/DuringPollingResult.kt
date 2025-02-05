package org.amine.security.polling.onlinepollingsystem.models.polling

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.ZonedDateTime

@Entity
class DuringPollingResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var resultId:Long=0
    var pollId: Long = 0
    var userId: Long = 0
    var titleOfPoll: String = ""
    var descriptionPoll: String = ""
    var selectedOptions: String = ""
    var timeOfPolling: ZonedDateTime? = null
    var startTimeOfPolling: ZonedDateTime? = null
    var endTimeOfPolling: ZonedDateTime? = null
}