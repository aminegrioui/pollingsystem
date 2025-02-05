package org.amine.security.polling.onlinepollingsystem.repos.user


import org.amine.security.polling.onlinepollingsystem.models.users.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UserRepository: JpaRepository<User,Long> {
    fun findByUsername(username:String?):Optional<User>
    fun findByEmail(email: String?): Optional<User>
}