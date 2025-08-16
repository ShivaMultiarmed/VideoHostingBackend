package mikhail.shell.video.hosting.dto

import mikhail.shell.video.hosting.domain.Liking
import mikhail.shell.video.hosting.domain.Video
import mikhail.shell.video.hosting.domain.VideoWithUser
import java.time.Instant

data class VideoDto(
    val videoId: Long? = null,
    val channelId: Long,
    val title: String,
    val dateTime: Instant? = null,
    val views: Long = 0,
    val likes: Long = 0,
    val dislikes: Long = 0,
    val sourceUrl: String? = null,
    val coverUrl: String? = null
)

fun Video.toDto(
    sourceUrl: String? = null,
    coverUrl: String? = null
) = VideoDto(
    videoId = videoId,
    channelId = channelId,
    title = title,
    dateTime = dateTime,
    views = views,
    likes = likes,
    dislikes = dislikes,
    sourceUrl = sourceUrl,
    coverUrl = coverUrl
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
    val videoId: Long? = null,
    val channelId: Long,
    val title: String,
    val dateTime: Instant? = null,
    val views: Long,
    val likes: Long,
    val liking: Liking,
    val dislikes: Long,
    val sourceUrl: String? = null,
    val coverUrl: String? = null
)

fun VideoWithUser.toDto(
    sourceUrl: String? = null,
    coverUrl: String? = null
): VideoWithUserDto {
    return VideoWithUserDto(
        videoId = videoId,
        channelId = channelId,
        title = title,
        dateTime = dateTime,
        views = views,
        likes = likes,
        liking = liking,
        dislikes = dislikes,
        sourceUrl = sourceUrl,
        coverUrl = coverUrl
    )
}
