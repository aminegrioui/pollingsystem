package org.amine.security.polling.onlinepollingsystem.models.permission

import jakarta.persistence.*
import org.amine.security.polling.onlinepollingsystem.models.users.User


@Entity
class Permission() {
    @Id
    @Column(name = "permession_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    var permission: String = ""

    @ManyToMany(mappedBy = "permissions")
    var users: MutableSet<User> = mutableSetOf()

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (other === this) {
            return true
        }
        if (javaClass != other.javaClass) {
            return false
        }

        val permission1: Permission = other as Permission

        return (permission == permission1.permission)
    }

    override fun hashCode(): Int {
        val PRIME = 31
        var result = 32
        result = PRIME * permission.length * result
        return result
    }

    override fun toString(): String {
        return permission
    }
}