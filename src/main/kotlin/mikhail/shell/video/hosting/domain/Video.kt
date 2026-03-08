package mikhail.shell.video.hosting.domain

import java.time.Instant
import java.util.UUID

data class Video(
    val videoId: Long,
    val channelId: Long,
    val title: String,
    val description: String?,
    val dateTime: Instant,
    val views: Long,
    val likes: Long,
    val dislikes: Long
)

class VideoWithChannel(
    val video: Video,
    val channel: Channel
)

data class VideoWithUser(
    val videoId: Long,
    val channelId: Long,
    val title: String,
    val description: String?,
    val dateTime: Instant,
    val views: Long,
    val likes: Long,
    val dislikes: Long,
    val liking: Liking
)

infix fun Video.with(liking: Liking) = VideoWithUser(
    videoId = videoId,
    channelId = channelId,
    title = title,
    description = description,
    dateTime = dateTime,
    views = views,
    likes = likes,
    dislikes = dislikes,
    liking = liking
)

data class VideoCreationModel(
    val channelId: Long,
    val title: String,
    val description: String?,
    val cover: UploadedFile?,
    val source: VideoMetaData
)

data class PendingVideo(
    val tmpId: UUID,
    val channelId: Long
)

data class VideoMetaData(
    val fileName: String,
    val mimeType: String,
    val size: Long
)

data class VideoEditingModel(
    val videoId: Long,
    val title: String,
    val description: String?,
    val cover: EditingAction<UploadedFile>
)

enum class Liking {
    LIKED, DISLIKED, NONE
}
