package mikhail.shell.video.hosting.domain

import java.time.LocalDateTime

class Video(
    val videoId: Long,
    val channelId: Long,
    val title: String,
    val dateTime: LocalDateTime,
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
    val dateTime: LocalDateTime,
    val views: Long,
    val likes: Long,
    val dislikes: Long,
    val liking: LikingState
)

