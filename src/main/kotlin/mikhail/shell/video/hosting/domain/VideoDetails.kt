package mikhail.shell.video.hosting.domain

import mikhail.shell.video.hosting.dto.ExtendedChannelInfo
import mikhail.shell.video.hosting.dto.ExtendedVideoInfo

data class VideoDetails(
    val video: ExtendedVideoInfo,
    val channel: ExtendedChannelInfo
)
