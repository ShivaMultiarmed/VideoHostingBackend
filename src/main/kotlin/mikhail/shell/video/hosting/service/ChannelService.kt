package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.*
import org.springframework.core.io.Resource

interface ChannelService {
    fun get(channelId: Long): Channel
    fun getForUser(channelId: Long, userId: Long): ChannelWithUser
    fun checkIfSubscribed(channelId: Long, userId: Long): Boolean
    fun createChannel(
        channel: Channel,
        logo: UploadedFile?,
        header: UploadedFile?
    ): Channel
    fun getChannelsByOwnerId(userId: Long): List<Channel>
    fun getChannelsBySubscriberId(userId: Long): List<Channel>
    fun changeSubscriptionState(
        subscriberId: Long,
        channelId: Long,
        subscription: Subscription,
        token: String
    ): ChannelWithUser

    fun subscribeToNotifications(userId: Long, token: String)
    fun unsubscribeFromNotifications(userId: Long, token: String)
    fun getChannel(channelId: Long): Channel
    fun removeChannel(channelId: Long)
    fun checkOwner(userId: Long, channelId: Long): Boolean
    fun checkExistence(channelId: Long): Boolean
    fun getLogo(channelId: Long): Resource
    fun getHeader(channelId: Long): Resource
    fun editChannel(
        channel: Channel,
        header: UploadedFile?,
        headerAction: EditAction,
        logo: UploadedFile?,
        logoAction: EditAction
    ): Channel
}