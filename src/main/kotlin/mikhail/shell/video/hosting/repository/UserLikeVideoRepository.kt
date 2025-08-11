package mikhail.shell.video.hosting.repository

import mikhail.shell.video.hosting.repository.entities.VideoLiking
import mikhail.shell.video.hosting.repository.entities.VideoLikingId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserLikeVideoRepository : JpaRepository<VideoLiking, VideoLikingId>