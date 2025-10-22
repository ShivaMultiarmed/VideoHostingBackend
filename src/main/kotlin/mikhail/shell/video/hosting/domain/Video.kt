package mikhail.shell.video.hosting.domain

import mikhail.shell.video.hosting.controllers.VideoMetaData
import java.time.Instant

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