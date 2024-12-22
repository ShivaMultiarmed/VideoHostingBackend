package mikhail.shell.video.hosting.repository.models

import jakarta.persistence.*
import mikhail.shell.video.hosting.domain.VideoInfo
import java.time.LocalDateTime

@Entity
@Table(name = "videos")
data class VideoEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val videoId: Long,
    val channelId: Long,
    val title: String,
    val dateTime: LocalDateTime,
    val views: Long,
    val likes: Long,
    val dislikes: Long
)

fun VideoEntity.toDomain() = VideoInfo(videoId, channelId, title, dateTime, views, likes, dislikes)
fun VideoInfo.toDomain() = VideoEntity(videoId, channelId, title, dateTime, views, likes, dislikes)