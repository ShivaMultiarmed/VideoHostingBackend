package mikhail.shell.video.hosting.entities

import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "subscribers")
data class Subscriber(
    @EmbeddedId
    val id: SubscriberId
)

@Embeddable
data class SubscriberId(
    val channelId: Long,
    val userId: Long
)