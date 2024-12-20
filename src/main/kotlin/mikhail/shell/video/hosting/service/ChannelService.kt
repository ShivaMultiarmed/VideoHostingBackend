package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.ChannelInfo
import mikhail.shell.video.hosting.dto.ExtendedChannelInfo

interface ChannelService {
    fun provideChannelInfo(
        channelId: Long
    ): ChannelInfo

    fun getExtendedChannelInfo(
       channelId: Long,
       userId: Long
    ): ExtendedChannelInfo
}