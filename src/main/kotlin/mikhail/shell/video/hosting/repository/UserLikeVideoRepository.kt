package mikhail.shell.video.hosting.repository

import mikhail.shell.video.hosting.repository.entities.UserLikeVideo
import mikhail.shell.video.hosting.repository.entities.UserLikeVideoId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserLikeVideoRepository : JpaRepository<UserLikeVideo, UserLikeVideoId>