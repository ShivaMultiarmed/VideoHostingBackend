package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.Channel
import mikhail.shell.video.hosting.domain.ChannelWithUser

interface ChannelService {
    fun provideChannelInfo(
        channelId: Long
    ): Channel
    fun provideChannelForUser(
        channelId: Long,
        userId: Long
    ): ChannelWithUser
    fun checkIfSubscribed(
        channelId: Long,
        userId: String
    ): Boolean
    fun createChannel(
        channel: Channel
    ): Channel
}