package org.amine.security.polling.onlinepollingsystem.models.users

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import org.amine.security.polling.onlinepollingsystem.models.admin.Admin
import org.amine.security.polling.onlinepollingsystem.models.permission.Permission
import org.amine.security.polling.onlinepollingsystem.models.polling.Poll
import java.util.*
import kotlin.collections.HashSet

@Entity
@Table(name = "app_user")
class User() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

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
        name = "user_permissions",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "permission_id")]
    )
    var permissions: MutableSet<Permission> = HashSet()

    @OneToMany(
        cascade = [CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH],
        fetch = FetchType.EAGER,
        mappedBy = "creator"
    )
    var polls: MutableSet<Poll> = mutableSetOf()

    @ManyToOne
    @JoinColumn(name = "admin_id")
    var admin: Admin? = null

    @ManyToMany(
        fetch = FetchType.LAZY,
        cascade = [CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH]
    )
    @JoinTable(
        name = "user_participated_polls",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "poll_id")]
    )
    var participatedPolls: MutableSet<Poll> = mutableSetOf()

    override
    fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        return (Objects.equals(username, other.username)) && (Objects.equals(id, other.id))
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + username.hashCode()
        return result
    }


}