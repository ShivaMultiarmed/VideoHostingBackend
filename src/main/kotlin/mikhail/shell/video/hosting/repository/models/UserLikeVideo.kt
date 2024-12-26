package mikhail.shell.video.hosting.repository.models

import jakarta.persistence.*
import mikhail.shell.video.hosting.domain.LikingState
import java.io.Serializable

@Entity
@Table(name = "users_like_videos")
data class UserLikeVideo(
    @EmbeddedId val id: UserLikeVideoId,
    @Enumerated(value = EnumType.STRING)
    val likingState: LikingState
)

@Embeddable
data class UserLikeVideoId(
    val userId: Long,
    val videoId: Long,
): Serializable

