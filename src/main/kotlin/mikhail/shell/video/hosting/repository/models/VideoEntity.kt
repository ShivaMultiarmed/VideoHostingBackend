package mikhail.shell.video.hosting.repository.models

import jakarta.persistence.*
import mikhail.shell.video.hosting.domain.LikingState
import mikhail.shell.video.hosting.domain.Video
import mikhail.shell.video.hosting.domain.VideoWithChannel
import java.time.LocalDateTime

@Entity
//@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "videos")
class VideoEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "video_id") val videoId: Long? = null,
    @Column(name = "channel_id") val channelId: Long,
    val title: String,
    val dateTime: LocalDateTime = LocalDateTime.now(),
    val views: Long = 0,
    val likes: Long = 0,
    val dislikes: Long = 0
)

fun VideoEntity.toDomain() = Video(
    videoId,
    channelId,
    title,
    dateTime,
    views,
    likes,
    dislikes
)
fun Video.toEntity() = VideoEntity(
    videoId,
    channelId,
    title,
    dateTime,
    views,
    likes,
    dislikes
)

@Entity
@Table(name = "videos")
class VideoWithChannelEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "video_id") val videoId: Long? = null,
    @Column(name = "channel_id") val channelId: Long,
    val title: String,
    val dateTime: LocalDateTime,
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

//    : VideoEntity(
//    videoId,
//    channelId,
//    title,
//    dateTime,
//    views,
//    likes,
//    dislikes
//)

fun VideoWithChannelEntity.toDomain() = VideoWithChannel(
    video = Video(
        videoId,
        channelId,
        title,
        dateTime,
        views,
        likes,
        dislikes
    ),
    channel = channel.toDomain()
)
fun VideoWithChannel.toEntity() = VideoWithChannelEntity(
    video.videoId,
    video.channelId,
    video.title,
    video.dateTime,
    video.views,
    video.likes,
    video.dislikes,
    channel.toEntity()
)