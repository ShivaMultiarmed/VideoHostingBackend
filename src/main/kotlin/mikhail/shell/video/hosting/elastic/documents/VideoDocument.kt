package mikhail.shell.video.hosting.elastic.documents

import mikhail.shell.video.hosting.repository.entities.VideoState
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
