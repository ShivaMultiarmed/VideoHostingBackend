package mikhail.shell.video.hosting.elastic.documents

import mikhail.shell.video.hosting.entities.VideoEntity
import mikhail.shell.video.hosting.entities.VideoState
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.DateFormat
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.time.Instant

@Document(indexName = "videos")
data class VideoDocument(
    @Id val videoId: Long,
    val channelId: Long,
    val title: String,
    @Field(type = FieldType.Date, format = [DateFormat.epoch_millis])
    val dateTime: Instant,
    val views: Long,
    val likes: Long,
    val dislikes: Long,
    val state: VideoState
)

fun VideoEntity.toDocument() = VideoDocument(
    videoId = videoId!!,
    channelId = channelId,
    title = title,
    dateTime = dateTime,
    views = views,
    likes = likes,
    dislikes = dislikes,
    state = state
)

fun VideoDocument.toEntity() = VideoEntity(
    videoId = videoId,
    channelId = channelId,
    title = title,
    dateTime = dateTime,
    views = views,
    likes = likes,
    dislikes = dislikes,
    state = state
)