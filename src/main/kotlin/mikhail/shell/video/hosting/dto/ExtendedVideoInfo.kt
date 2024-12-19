package mikhail.shell.video.hosting.dto

import mikhail.shell.video.hosting.domain.VideoInfo

data class ExtendedVideoInfo(
    val videoInfo: VideoInfo,
    val liking: Boolean?
)
