package mikhail.shell.video.hosting.repository.entities

import jakarta.persistence.*
import mikhail.shell.video.hosting.domain.Video
import mikhail.shell.video.hosting.domain.VideoWithChannel
import java.time.Instant

enum class VideoState {
    CREATED, UPLOADED
}

@Entity
@Table(name = "videos")
data class VideoEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val videoId: Long? = null,
    val channelId: Long,
    val title: String,
    val dateTime: Instant? = null,
    val views: Long = 0,
    val likes: Long = 0,
    val dislikes: Long = 0,
    @Enumerated(value = EnumType.STRING)
    val state: VideoState
)


fun VideoEntity.toDomain() = Video(
    videoId = videoId,
    channelId = channelId,
    title = title,
    dateTime = dateTime!!,
    views = views,
    likes = likes,
    dislikes = dislikes
)
fun Video.toEntity(state: VideoState = VideoState.CREATED) = VideoEntity(
    videoId = videoId,
    channelId = channelId,
    title = title,
    dateTime = dateTime!!,
    views = views,
    likes = likes,
    dislikes = dislikes,
    state = state
)

@Entity
@Table(name = "videos")
data class VideoWithChannelEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "video_id") val videoId: Long? = null,
    @Column(name = "channel_id") val channelId: Long,
    val title: String,
    val dateTime: Instant?,
    val views: Long,
    val likes: Long,
    val dislikes: Long,
    @Enumerated(value = EnumType.STRING)
    val state: VideoState,
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
        videoId = videoId,
        channelId = channelId,
        title = title,
        dateTime = dateTime!!,
        views = views,
        likes = likes,
        dislikes = dislikes
    ),
    channel = channel.toDomain()
)
fun VideoWithChannel.toEntity(
    state: VideoState
) = VideoWithChannelEntity(
    videoId = video.videoId,
    channelId = video.channelId,
    title = video.title,
    dateTime = video.dateTime!!,
    views = video.views,
    likes = video.likes,
    dislikes = video.dislikes,
    state = state,
    channel = channel.toEntity()
)