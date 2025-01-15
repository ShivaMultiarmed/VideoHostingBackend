package mikhail.shell.video.hosting.repository

import mikhail.shell.video.hosting.repository.models.VideoEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository("videoRepository_mysql")
interface VideoRepository: JpaRepository<VideoEntity, Long> {
    fun findByTitle(
        title: String,
        pageable: Pageable
    ): List<VideoEntity>
    fun findByChannelIdOrderByDateTimeDesc(
        channelId: Long,
        pageable: Pageable
    ): List<VideoEntity>
}