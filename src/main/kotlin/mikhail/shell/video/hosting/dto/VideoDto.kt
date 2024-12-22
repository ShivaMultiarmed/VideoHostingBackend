package mikhail.shell.video.hosting.dto

import mikhail.shell.video.hosting.domain.LikingState
import mikhail.shell.video.hosting.domain.VideoInfo
import java.time.LocalDateTime

data class VideoDto(
    val videoId: Long,
    val channelId: Long,
    val title: String,
    val dateTime: LocalDateTime,
    val views: Long,
    val likes: Long,
    val dislikes: Long,
    val liking: LikingState,
    val sourceUrl: String,
    val coverUrl: String
)

fun VideoInfo.toDto(
    liking: LikingState,
    sourceUrl: String,
    coverUrl: String
) = VideoDto(
    videoId,
    channelId,
    title,
    dateTime,
    views,
    likes,
    dislikes,
    liking,
    sourceUrl,
    coverUrl
)

fun VideoDto.toDomain() = VideoInfo(
    videoId,
    channelId,
    title,
    dateTime,
    views,
    likes,
    dislikes
)
