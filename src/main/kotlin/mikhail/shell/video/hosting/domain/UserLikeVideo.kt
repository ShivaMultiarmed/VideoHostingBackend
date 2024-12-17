package mikhail.shell.video.hosting.domain

import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.io.Serializable

@Entity
@Table(name = "users_like_videos")
data class UserLikeVideo(
    @EmbeddedId val id: UserLikeVideoId,
    val liking: Boolean
)

@Embeddable
data class UserLikeVideoId(
    val userId: Long,
    val videoId: Long,
): Serializable

