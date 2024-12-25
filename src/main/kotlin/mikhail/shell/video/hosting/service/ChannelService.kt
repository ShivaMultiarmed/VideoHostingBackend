package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.Channel

interface ChannelService {
    fun provideChannelInfo(
        channelId: Long
    ): Channel
    fun checkIfSubscribed(
        channelId: Long,
        userId: String
    ): Boolean
}