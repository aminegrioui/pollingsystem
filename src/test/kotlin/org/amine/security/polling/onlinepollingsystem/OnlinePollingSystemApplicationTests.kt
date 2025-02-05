package org.amine.security.polling.onlinepollingsystem



fun main(args: Array<String>) {

    val oldPollStatus = "FINISHED"
    val setOfStatus = setOf("PENDING", "CANCELED", "FINISHED","OPEN_ACTIVE").minus(oldPollStatus)
    println(setOfStatus)
}
