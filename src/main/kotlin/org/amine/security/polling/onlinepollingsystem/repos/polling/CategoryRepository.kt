package org.amine.security.polling.onlinepollingsystem.repos.polling;

import org.amine.security.polling.onlinepollingsystem.models.polling.Category
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface CategoryRepository : JpaRepository<Category, Long> {
    fun findByCategoryName(name: String): Optional<Category>
}