package mikhail.shell.video.hosting.dto

import mikhail.shell.video.hosting.domain.ChannelInfo

data class ExtendedChannelInfo(
    val info: ChannelInfo,
    val subscription: Boolean
)
