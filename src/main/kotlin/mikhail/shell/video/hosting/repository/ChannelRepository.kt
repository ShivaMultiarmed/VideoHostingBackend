package mikhail.shell.video.hosting.repository

import mikhail.shell.video.hosting.repository.models.ChannelEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChannelRepository : JpaRepository<ChannelEntity, Long> {
    fun findByOwnerId(ownerId: Long): List<ChannelEntity>
    fun existsByTitle(title: String): Boolean
}