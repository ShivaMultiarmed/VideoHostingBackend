package mikhail.shell.video.hosting.repository

import mikhail.shell.video.hosting.repository.models.Subscriber
import mikhail.shell.video.hosting.repository.models.SubscriberId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SubscriberRepository: JpaRepository<Subscriber, SubscriberId> {
    fun findById_UserId(userId: Long): List<Subscriber>
}