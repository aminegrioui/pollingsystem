package org.amine.security.polling.onlinepollingsystem.models.polling

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import org.amine.security.polling.onlinepollingsystem.enumuration.polling.PollType
import org.amine.security.polling.onlinepollingsystem.enumuration.polling.VisibilityType
import org.amine.security.polling.onlinepollingsystem.models.admin.Admin
import org.amine.security.polling.onlinepollingsystem.models.users.User
import java.time.ZonedDateTime
import java.util.*

@Entity
class Poll {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    var title: String = ""

    var description: String = ""

    var startPollingTime: ZonedDateTime? = null

    var endPollingTime: ZonedDateTime? = null

    var createdPollTime: ZonedDateTime? = null

    var updatedPollTime: ZonedDateTime? = null

    @ManyToOne
    @JoinColumn(name = "user_id")
    var creator: User? = null

    @ManyToMany(
        fetch = FetchType.LAZY,
        cascade = [CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH]
    )
    var participants: MutableSet<User> = mutableSetOf()


    var visibility: String = VisibilityType.PUBLIC.name

    var type: String = PollType.MULTIPLE_CHOICE.name

    @ManyToOne
    var category: Category = Category()

    @ElementCollection
    var options: Set<String> = emptySet()

    @JsonIgnore
    var deleted: Boolean = false

    var isPublicResults: Boolean? = true

    // status of poll
    var status: String = Status.OPEN_ACTIVE.name

    // state of result of poll
    var resultState: String = ResultPollingStates.AWAITING.name

    override
    fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Poll

        return (Objects.equals(title, other.title)) && (Objects.equals(description, other.description))
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + description.hashCode()
        return result
    }
}