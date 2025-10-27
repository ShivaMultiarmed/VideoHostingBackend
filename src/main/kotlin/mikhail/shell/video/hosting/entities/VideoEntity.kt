package mikhail.shell.video.hosting.entities

import jakarta.persistence.*
import mikhail.shell.video.hosting.domain.Video
import mikhail.shell.video.hosting.domain.VideoWithChannel
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "videos")
data class VideoEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "video_id")
    val videoId: Long? = null,
    @Column(name = "channel_id")
    val channelId: Long,
    val title: String,
    val description: String?,
    @Column(name = "date_time")
    val dateTime: Instant,
    val views: Long = 0,
    val likes: Long = 0,
    val dislikes: Long = 0
)

fun VideoEntity.toDomain() = Video(
    videoId = videoId!!,
    channelId = channelId,
    title = title,
    description = description,
    dateTime = dateTime,
    views = views,
    likes = likes,
    dislikes = dislikes
)
fun Video.toEntity() = VideoEntity(
    videoId = videoId,
    channelId = channelId,
    title = title,
    description = description,
    dateTime = dateTime,
    views = views,
    likes = likes,
    dislikes = dislikes
)

@Entity
@Table(name = "videos")
data class VideoWithChannelEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "video_id") val videoId: Long? = null,
    @Column(name = "channel_id") val channelId: Long,
    val title: String,
    val description: String?,
    @Column(name = "date_time")
    val dateTime: Instant,
    val views: Long,
    val likes: Long,
    val dislikes: Long,
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
        name = "channel_id",
        referencedColumnName = "channel_id",
        insertable = false,
        updatable = false
    )
    val channel: ChannelEntity
)

fun VideoWithChannelEntity.toDomain() = VideoWithChannel(
    video = Video(
        videoId = videoId!!,
        channelId = channelId,
        title = title,
        description = description,
        dateTime = dateTime,
        views = views,
        likes = likes,
        dislikes = dislikes
    ),
    channel = channel.toDomain()
)
fun VideoWithChannel.toEntity() = VideoWithChannelEntity(
    videoId = video.videoId,
    channelId = video.channelId,
    title = video.title,
    description = video.description,
    dateTime = video.dateTime,
    views = video.views,
    likes = video.likes,
    dislikes = video.dislikes,
    channel = channel.toEntity()
)

@Entity
@Table(name = "pending_videos")
data class PendingVideoEntity(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val tmpId: UUID = UUID.randomUUID(),
    val channelId: Long,
    val title: String,
    val description: String?,
    val dateTime: Instant,
    val fileName: String,
    val mimeType: String,
    val size: Long
)