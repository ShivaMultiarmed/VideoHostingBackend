package mikhail.shell.video.hosting.domain

import java.time.Instant

data class Video(
    val videoId: Long? = null,
    val channelId: Long,
    val title: String,
    val dateTime: Instant? = null,
    val views: Long = 0,
    val likes: Long = 0,
    val dislikes: Long = 0
)

class VideoWithChannel(
    val video: Video,
    val channel: Channel
)

data class VideoWithUser(
    val videoId: Long? = null,
    val channelId: Long,
    val title: String,
    val dateTime: Instant? = null,
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
