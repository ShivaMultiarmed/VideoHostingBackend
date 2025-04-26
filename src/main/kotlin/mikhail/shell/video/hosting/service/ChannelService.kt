package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.dto.ChannelDto
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile

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
        userId: Long
    ): Boolean
    fun createChannel(
        channel: Channel,
        avatar: File?,
        cover: File?
    ): Channel
    fun getChannelsByOwnerId(
        userId: Long
    ): List<Channel>
    fun getChannelsBySubscriberId(
        userId: Long
    ): List<Channel>
    fun changeSubscriptionState(
        subscriberId: Long,
        channelId: Long,
        token: String,
        subscriptionState: SubscriptionState
    ): ChannelWithUser
    fun resubscribe(
        userId: Long,
        token: String
    )
    fun editChannel(
        channel: Channel,
        editCoverAction: EditAction,
        coverFile: File?,
        editAvatarAction: EditAction,
        avatarFile: File?
    ): Channel
    fun getChannel(channelId: Long): Channel
    fun removeChannel(channelId: Long)
    fun checkOwner(userId: Long, channelId: Long): Boolean
}