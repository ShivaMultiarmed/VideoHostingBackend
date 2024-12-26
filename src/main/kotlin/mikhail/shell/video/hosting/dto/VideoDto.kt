package mikhail.shell.video.hosting.dto

import mikhail.shell.video.hosting.domain.LikingState
import mikhail.shell.video.hosting.domain.Video
import mikhail.shell.video.hosting.domain.VideoWithChannel
import mikhail.shell.video.hosting.domain.VideoWithUser
import java.time.LocalDateTime

data class VideoDto(
    val videoId: Long,
    val channelId: Long,
    val title: String,
    val dateTime: LocalDateTime,
    val views: Long,
    val likes: Long,
    val dislikes: Long,
    val sourceUrl: String? = null,
    val coverUrl: String? = null
)

fun Video.toDto(
    sourceUrl: String? = null,
    coverUrl: String? = null
) = VideoDto(
    videoId,
    channelId,
    title,
    dateTime,
    views,
    likes,
    dislikes,
    sourceUrl,
    coverUrl
)

fun VideoDto.toDomain() = Video(
    videoId,
    channelId,
    title,
    dateTime,
    views,
    likes,
    dislikes
)

data class VideoWithChannelDto(
    val video: VideoDto,
    val channel: ChannelDto
)


data class VideoWithUserDto(
    val videoId: Long,
    val channelId: Long,
    val title: String,
    val dateTime: LocalDateTime,
    val views: Long,
    val likes: Long,
    val liking: LikingState,
    val dislikes: Long,
    val sourceUrl: String? = null,
    val coverUrl: String? = null
)

fun VideoWithUser.toDto(
    sourceUrl: String? = null,
    coverUrl: String? = null
): VideoWithUserDto {
    return VideoWithUserDto(
        videoId,
        channelId,
        title,
        dateTime,
        views,
        likes,
        liking,
        dislikes,
        sourceUrl,
        coverUrl
    )
}
