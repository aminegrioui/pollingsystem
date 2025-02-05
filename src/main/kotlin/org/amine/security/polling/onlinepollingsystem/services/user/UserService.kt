package org.amine.security.polling.onlinepollingsystem.services.user


import org.amine.security.polling.onlinepollingsystem.dtos.user.response.SmallUserDto
import org.amine.security.polling.onlinepollingsystem.exceptions.ResourceNotFoundException
import org.amine.security.polling.onlinepollingsystem.exceptions.ValidationDataException
import org.amine.security.polling.onlinepollingsystem.models.users.User
import org.amine.security.polling.onlinepollingsystem.repos.user.UserRepository
import org.amine.security.polling.onlinepollingsystem.system.tools.AppTool
import org.springframework.stereotype.Service
import java.util.logging.Logger

@Service
class UserService(
    val appTool: AppTool,
    var userRepository: UserRepository,
) {
    val logger = Logger.getLogger(UserService::class.java.name)

    fun findAllUsers(): MutableList<SmallUserDto> {
        return userRepository.findAll().stream().map { SmallUserDto(it.id, it.username) }.toList()
    }

    fun allUsersAsSmallUserDto(users: Set<User>): List<SmallUserDto> {
        return users.map { SmallUserDto(it.id, it.username) }.toList()
    }

    fun deleteUserById(userId: Long): Boolean {
        val optional = userRepository.findById(userId)
        if (optional.isEmpty) {
            throw ResourceNotFoundException("User not found with id $userId")
        }
        val user = optional.get()
        if (user.isDeleted) {
            throw ValidationDataException("User already deleted with id $userId")
        }
        user.isDeleted = true
        userRepository.save(user)
        return true
    }

    fun findUserById(id: Long): SmallUserDto {
        val optionalUser = userRepository.findById(id)
        if (optionalUser.isEmpty) {
            throw ResourceNotFoundException("User not found with id $id")
        }
        return optionalUser.map { SmallUserDto(it.id, it.username) }.get()
    }
}