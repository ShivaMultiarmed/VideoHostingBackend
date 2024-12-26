package mikhail.shell.video.hosting.domain

import java.time.LocalDateTime

class Video(
    val videoId: Long? = null,
    val channelId: Long,
    val title: String,
    val dateTime: LocalDateTime = LocalDateTime.now(),
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
    val dateTime: LocalDateTime,
    val views: Long,
    val likes: Long,
    val dislikes: Long,
    val liking: LikingState
)

