package mikhail.shell.video.hosting.repository

import mikhail.shell.video.hosting.dto.ExtendedVideoInfo
import mikhail.shell.video.hosting.domain.VideoInfo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VideoRepository: JpaRepository<VideoInfo, Long> {
    @Query(
        value = "SELECT new mikhail.shell.video.hosting.dto.ExtendedVideoInfo(" +
                "new mikhail.shell.video.hosting.domain.VideoInfo(" +
                "v.videoId, v.channelId, v.title, v.dateTime, v.views, v.likes, v.dislikes" +
                ")," +
                "l.liking" +
                ")" +
                " FROM VideoInfo v LEFT JOIN UserLikeVideo l" +
                " ON v.videoId = l.id.videoId" +
                " WHERE v.videoId = :video_id AND l.id.userId = :user_id",
    )
    fun findByVideoIdAndUserId(
        @Param("video_id") videoId: Long,
        @Param("user_id") userId: Long
    ): Optional<ExtendedVideoInfo>
}