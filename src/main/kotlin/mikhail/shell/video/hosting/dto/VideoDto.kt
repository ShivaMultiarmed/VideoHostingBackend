package mikhail.shell.video.hosting.dto

import mikhail.shell.video.hosting.domain.Liking
import mikhail.shell.video.hosting.domain.Video
import mikhail.shell.video.hosting.domain.VideoWithUser
import java.time.Instant
import java.util.UUID

data class VideoDto(
    val videoId: Long,
    val channelId: Long,
    val title: String,
    val dateTime: Instant,
    val views: Long,
    val likes: Long,
    val dislikes: Long,
    val description: String?
)

fun Video.toDto() = VideoDto(
    videoId = videoId,
    channelId = channelId,
    title = title,
    dateTime = dateTime,
    views = views,
    likes = likes,
    dislikes = dislikes,
    description = description
)

fun VideoDto.toDomain() = Video(
    videoId = videoId,
    channelId = channelId,
    title = title,
    dateTime = dateTime,
    views = views,
    likes = likes,
    dislikes = dislikes,
    description = description
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

data class PendingVideoDto(
    val tmpId: UUID
)