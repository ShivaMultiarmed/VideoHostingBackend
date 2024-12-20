package mikhail.shell.video.hosting.domain

import jakarta.persistence.*
import java.time.LocalDateTime
import kotlin.jvm.Transient

@Entity
@Table(name = "videos")
data class VideoInfo(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val videoId: Long,
    val channelId: Long,
    val title: String,
    val dateTime: LocalDateTime,
    val views: Long,
    val likes: Long,
    val dislikes: Long,
//    @Transient
//    val coverUrl: String? = null
)
