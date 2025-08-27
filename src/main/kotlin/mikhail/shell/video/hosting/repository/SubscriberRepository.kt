package mikhail.shell.video.hosting.repository

import mikhail.shell.video.hosting.entities.Subscriber
import mikhail.shell.video.hosting.entities.SubscriberId
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SubscriberRepository: JpaRepository<Subscriber, SubscriberId> {
    fun findById_UserId(
        userId: Long
    ): List<Subscriber>
    fun findById_UserId(
        userId: Long,
        pageable: Pageable
    ): List<Subscriber>
}