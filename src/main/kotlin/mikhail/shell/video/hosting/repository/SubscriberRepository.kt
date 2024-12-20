package mikhail.shell.video.hosting.repository

import mikhail.shell.video.hosting.domain.Subscriber
import mikhail.shell.video.hosting.domain.SubscriberId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SubscriberRepository: JpaRepository<Subscriber, SubscriberId>