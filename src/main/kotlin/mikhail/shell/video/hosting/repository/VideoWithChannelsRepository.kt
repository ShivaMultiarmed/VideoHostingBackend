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
            LEFT OUTER JOIN Subscriber s ON v.channelId = s.id.channelId AND s.id.userId = :user_id 
            WHERE v.state = 'UPLOADED' 
            ORDER BY (
            :date_time_weight * (CAST(FUNCTION('unix_timestamp', v.dateTime) AS Long) + 0.0) + 
            :subscribers_weight * (v.channel.subscribers + 0.0) +  
            :views_weight * (v.views + 0.0) + 
            :likes_weight * (v.likes + 0.0) +  
            :dislikes_weight * (v.dislikes + 0.0)) DESC
        """
    )
    fun findRecommendedVideos(
        @Param("user_id") userId: Long,
        @Param("date_time_weight") dateTimeWeight: Double,
        @Param("subscribers_weight") subscribersWeight: Double,
        @Param("views_weight") viewsWeight: Double,
        @Param("likes_weight") likesWeight: Double,
        @Param("dislikes_weight") dislikesWeight: Double,
        pageable: Pageable
    ): Page<VideoWithChannelEntity>
}