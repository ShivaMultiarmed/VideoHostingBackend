package mikhail.shell.video.hosting.entities

import jakarta.persistence.*
import mikhail.shell.video.hosting.domain.Liking
import java.io.Serializable

@Entity
@Table(name = "video_likings")
data class VideoLiking(
    @EmbeddedId val id: VideoLikingId,
    @Enumerated(value = EnumType.STRING)
    val liking: Liking
)

@Embeddable
data class VideoLikingId(
    val userId: Long,
    val videoId: Long,
): Serializable

