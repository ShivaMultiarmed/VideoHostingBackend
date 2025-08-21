package mikhail.shell.video.hosting.elastic.documents

import mikhail.shell.video.hosting.entities.VideoEntity
import mikhail.shell.video.hosting.entities.VideoState
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import java.time.Instant

@Document(indexName = "videos")
data class VideoDocument(
    @Id val videoId: Long? = null,
    val channelId: Long,
    val title: String,
    val dateTime: Instant? = null,
    val views: Long = 0,
    val likes: Long = 0,
    val dislikes: Long = 0,
    val state: VideoState
)

fun VideoEntity.toDocument() = VideoDocument(
    videoId = videoId,
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