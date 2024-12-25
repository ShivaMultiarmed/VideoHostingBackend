package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.ChannelInfo

interface ChannelService {
    fun provideChannelInfo(
        channelId: Long
    ): ChannelInfo
    fun checkIfSubscribed(
        channelId: Long,
        userId: String
    ): Boolean
}