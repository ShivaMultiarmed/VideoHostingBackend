package mikhail.shell.video.hosting.repository

import mikhail.shell.video.hosting.domain.UserLikeVideo
import mikhail.shell.video.hosting.domain.VideoDetails
import mikhail.shell.video.hosting.domain.VideoInfo
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface VideoRepository: JpaRepository<VideoInfo, Long> {
    @Query("""
        SELECT new mikhail.shell.video.hosting.domain.VideoDetails(
            new mikhail.shell.video.hosting.dto.ExtendedVideoInfo(
                new mikhail.shell.video.hosting.domain.VideoInfo (
                    v.videoId, v.channelId, v.title, v.dateTime, v.views, v.likes, v.dislikes
                ),
                l.liking
            ),
            new mikhail.shell.video.hosting.dto.ExtendedChannelInfo(
                new mikhail.shell.video.hosting.domain.ChannelInfo(
                    ch.channelId, ch.ownerId, ch.title, ch.alias, ch.description, ch.subscribers
                ),
                CASE WHEN s.id.userId IS NOT NULL THEN true ELSE false END
            )
        ) FROM VideoInfo v   
        LEFT JOIN UserLikeVideo l ON l.id.videoId = v.videoId
        INNER JOIN ChannelInfo ch ON ch.channelId = v.channelId
        LEFT JOIN Subscriber s ON s.id.channelId = ch.channelId
        WHERE ch.channelId IN :channelIds AND s.id.userId = :userId
        ORDER BY v.dateTime DESC
    """)
    fun findBySubscriptions(
        @Param("channelIds") channelIds: List<Long>,
        @Param("userId") userId: Long,
        pageable: Pageable
    ): List<VideoDetails>

    fun findByChannelId(
        channelId: Long,
        pageable: Pageable
    ): List<VideoInfo>
}