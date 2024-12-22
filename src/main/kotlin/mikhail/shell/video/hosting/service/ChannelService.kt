package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.ChannelInfo
import mikhail.shell.video.hosting.dto.ChannelDto

interface ChannelService {
    fun provideChannelInfo(
        channelId: Long
    ): ChannelInfo
    fun checkIfSubscribed(
        channelId: Long,
        userId: Long
    ): Boolean
}