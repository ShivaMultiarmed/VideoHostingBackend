package mikhail.shell.video.hosting.repository.entities

import jakarta.persistence.*
import mikhail.shell.video.hosting.domain.Video
import mikhail.shell.video.hosting.domain.VideoWithChannel
import org.springframework.data.elasticsearch.annotations.DateFormat
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.time.LocalDateTime

enum class VideoState {
    CREATED, UPLOADED
}

@Entity
@Table(name = "videos")
@Document(indexName = "videos")
data class VideoEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @org.springframework.data.annotation.Id
    @Column(name = "video_id") val videoId: Long? = null,
    @Column(name = "channel_id") val channelId: Long,
    @Field(type = FieldType.Text, analyzer = "custom_multilingual_analyzer", searchAnalyzer = "custom_multilingual_analyzer")
    val title: String,
    @Field(type = FieldType.Date, format = [DateFormat.date_hour_minute_second_millis])
    val dateTime: LocalDateTime? = null,
    val views: Long = 0,
    val likes: Long = 0,
    val dislikes: Long = 0,
    @Enumerated(value = EnumType.STRING)
    val state: VideoState
)


fun VideoEntity.toDomain() = Video(videoId, channelId, title, dateTime, views, likes, dislikes)
fun Video.toEntity(state: VideoState = VideoState.CREATED) = VideoEntity(videoId, channelId, title, dateTime, views, likes, dislikes, state)

@Entity
@Table(name = "videos")
data class VideoWithChannelEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "video_id") val videoId: Long? = null,
    @Column(name = "channel_id") val channelId: Long,
    val title: String,
    val dateTime: LocalDateTime?,
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
    video = Video(videoId, channelId, title, dateTime, views, likes, dislikes),
    channel = channel.toDomain()
)
fun VideoWithChannel.toEntity(
    state: VideoState
) = VideoWithChannelEntity(video.videoId, video.channelId, video.title, video.dateTime, video.views, video.likes, video.dislikes, state, channel.toEntity())