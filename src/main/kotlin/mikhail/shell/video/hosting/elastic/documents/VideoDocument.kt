package mikhail.shell.video.hosting.elastic.documents

import mikhail.shell.video.hosting.entities.VideoEntity
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.DateFormat
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.time.Instant

@Document(indexName = "videos")
data class VideoDocument(
    @Id
    @Field(name = "video_id")
    val videoId: Long,
    @Field(name = "channel_id")
    val channelId: Long,
    val title: String,
    val description: String?,
    @Field(
        name = "date_time",
        type = FieldType.Date,
        format = [DateFormat.strict_date_hour_minute_second]
    )
    val dateTime: Instant,
    val views: Long,
    val likes: Long,
    val dislikes: Long
)

fun VideoEntity.toDocument() = VideoDocument(
    videoId = videoId!!,
    channelId = channelId,
    title = title,
    description = description,
    dateTime = dateTime,
    views = views,
    likes = likes,
    dislikes = dislikes
)

fun VideoDocument.toEntity() = VideoEntity(
    videoId = videoId,
    channelId = channelId,
    title = title,
    description = description,
    dateTime = dateTime,
    views = views,
    likes = likes,
    dislikes = dislikes
)