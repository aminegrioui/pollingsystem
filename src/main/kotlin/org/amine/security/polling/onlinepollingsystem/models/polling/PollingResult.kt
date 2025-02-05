package org.amine.security.polling.onlinepollingsystem.models.polling

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.ZonedDateTime

@Entity
class PollingResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var idResult: Long = 0

    var pollId: Long = 0

    var title: String = ""

    var description: String = ""

    var result: String = ""

    var vottingpercentage: Double = 0.0

    var timeOfResult: ZonedDateTime? = null
}