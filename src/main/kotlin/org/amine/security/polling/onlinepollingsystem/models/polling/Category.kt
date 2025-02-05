package org.amine.security.polling.onlinepollingsystem.models.polling

import jakarta.persistence.*
import org.amine.security.polling.onlinepollingsystem.models.polling.Poll

@Entity
class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    var categoryName: String = ""

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    var polls: MutableSet<Poll> = mutableSetOf()
}