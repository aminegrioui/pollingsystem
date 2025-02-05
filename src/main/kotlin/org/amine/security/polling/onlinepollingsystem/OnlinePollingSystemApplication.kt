package org.amine.security.polling.onlinepollingsystem

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class OnlinePollingSystemApplication

fun main(args: Array<String>) {

    runApplication<OnlinePollingSystemApplication>(*args)
}
