package mikhail.shell.video.hosting.repository

import mikhail.shell.video.hosting.entities.VideoEntity
import mikhail.shell.video.hosting.entities.VideoState
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface VideoRepository: JpaRepository<VideoEntity, Long> {
    fun findByChannelIdAndStateOrderByDateTimeDesc(
        channelId: Long,
        videoState: VideoState = VideoState.UPLOADED,
        pageable: Pageable
    ): List<VideoEntity>
    fun findByChannelId(
        channelId: Long
    ): List<VideoEntity>
}