package org.amine.security.polling.onlinepollingsystem.models.admin

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import org.amine.security.polling.onlinepollingsystem.models.permission.Permission
import org.amine.security.polling.onlinepollingsystem.models.users.User
import java.util.*
import kotlin.collections.HashSet

@Entity
class Admin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var adminId: Long = 0

    var username: String = ""

    var password: String = ""

    var email: String = ""

    var isEnabled: Boolean = false

    var isAccountNonLocked: Boolean = false

    var isCredentialsNonExpired: Boolean = false

    var isAccountNonExpired: Boolean = false

    @JsonIgnore
    var isDeleted: Boolean = false

    @ManyToMany(
        fetch = FetchType.EAGER,
        cascade = [CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH]
    )
    @JoinTable(
        name = "admin_permissions",
        joinColumns = [JoinColumn(name = "admin_id")],
        inverseJoinColumns = [JoinColumn(name = "permission_id")]
    )
    var permissions: MutableSet<Permission> = HashSet()

    @OneToMany(
        cascade = [CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH],
        fetch = FetchType.EAGER,
        mappedBy = "admin"
    )
    var users: MutableSet<User> = mutableSetOf()

    var parentIdOfAdmin: Long? = null

    override
    fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Admin

        return (Objects.equals(username, other.username)) && (Objects.equals(adminId, other.adminId))
    }

    override fun hashCode(): Int {
        var result = adminId.hashCode()
        result = 31 * result + username.hashCode()
        return result
    }

}