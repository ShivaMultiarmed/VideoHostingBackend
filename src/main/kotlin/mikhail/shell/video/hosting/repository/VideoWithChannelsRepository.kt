package mikhail.shell.video.hosting.repository

import mikhail.shell.video.hosting.repository.models.VideoWithChannelEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface VideoWithChannelsRepository: JpaRepository<VideoWithChannelEntity, Long> {
    fun findByTitleLike(title: String): List<VideoWithChannelEntity>
}