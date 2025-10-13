package mikhail.shell.video.hosting.dto

import mikhail.shell.video.hosting.domain.Liking
import mikhail.shell.video.hosting.domain.Video
import mikhail.shell.video.hosting.domain.VideoWithUser
import java.time.Instant

data class VideoDto(
    val videoId: Long,
    val channelId: Long,
    val title: String,
    val dateTime: Instant,
    val views: Long,
    val likes: Long,
    val dislikes: Long
)

fun Video.toDto() = VideoDto(
    videoId = videoId,
    channelId = channelId,
    title = title,
    dateTime = dateTime,
    views = views,
    likes = likes,
    dislikes = dislikes
)

fun VideoDto.toDomain() = Video(
    videoId = videoId,
    channelId = channelId,
    title = title,
    dateTime = dateTime,
    views = views,
    likes = likes,
    dislikes = dislikes
)

data class VideoWithChannelDto(
    val video: VideoDto,
    val channel: ChannelDto
)

data class VideoWithUserDto(
    val videoId: Long,
    val channelId: Long,
    val title: String,
    val dateTime: Instant,
    val views: Long,
    val likes: Long,
    val liking: Liking,
    val dislikes: Long
)

fun VideoWithUser.toDto() = VideoWithUserDto(
    videoId = videoId,
    channelId = channelId,
    title = title,
    dateTime = dateTime,
    views = views,
    likes = likes,
    liking = liking,
    dislikes = dislikes
)
