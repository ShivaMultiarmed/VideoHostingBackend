package mikhail.shell.video.hosting.repository

import mikhail.shell.video.hosting.repository.entities.VideoWithChannelEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface VideoWithChannelsRepository: JpaRepository<VideoWithChannelEntity, Long> {
    fun existsByChannel_OwnerIdAndVideoId(userId: Long, videoId: Long): Boolean
    @Query(
        value = """
            SELECT v FROM VideoWithChannelEntity v
            LEFT OUTER JOIN Subscriber s ON v.channelId = s.id.channelId AND s.id.userId = :userId 
            WHERE v.state = 'UPLOADED' 
            ORDER BY v.channel.subscribers DESC,  
            v.views DESC, 
            v.likes DESC,  
            v.dislikes DESC,  
            v.dateTime DESC
        """
    )
    fun findRecommendedVideos(@Param("userId") userId: Long, pageable: Pageable): Page<VideoWithChannelEntity>
}