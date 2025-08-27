package mikhail.shell.video.hosting.repository

import mikhail.shell.video.hosting.entities.ChannelEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ChannelRepository : JpaRepository<ChannelEntity, Long> {
    fun findByOwnerId(ownerId: Long): List<ChannelEntity>
    fun findByOwnerId(ownerId: Long, pageable: Pageable): List<ChannelEntity>
    fun existsByTitle(title: String): Boolean
    fun existsByOwnerIdAndChannelId(userId: Long, channelId: Long): Boolean
    fun existsByAlias(alias: String): Boolean
}