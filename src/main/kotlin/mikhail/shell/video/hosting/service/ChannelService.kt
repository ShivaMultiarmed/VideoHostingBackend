package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.Channel
import mikhail.shell.video.hosting.domain.ChannelWithUser
import mikhail.shell.video.hosting.domain.File

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
        channel: Channel,
        avatar: File?,
        cover: File?
    ): Channel
    fun getChannelsByOwnerId(
        userId: Long
    ): List<Channel>
}