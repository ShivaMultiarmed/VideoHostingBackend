package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.*
import org.springframework.core.io.Resource

interface ChannelService {
    fun get(channelId: Long): Channel
    fun getForUser(channelId: Long, userId: Long): ChannelWithUser
    fun checkIfSubscribed(channelId: Long, userId: Long): Boolean
    fun getChannelsByOwnerId(userId: Long): List<Channel>
    fun getChannelsByOwnerId(
        userId: Long,
        partIndex: Long,
        partSize: Int
    ): List<Channel>
    fun getSubscriptions(
        userId: Long,
        partIndex: Long,
        partSize: Int
    ): List<Channel>
    fun changeSubscriptionState(
        subscriberId: Long,
        channelId: Long,
        subscription: Subscription,
        token: String
    ): ChannelWithUser
    fun subscribeToNotifications(userId: Long, token: String)
    fun unsubscribeFromNotifications(userId: Long, token: String)
    fun getChannel(channelId: Long): Channel
    fun checkOwner(userId: Long, channelId: Long): Boolean
    fun existsById(channelId: Long): Boolean
    fun getLogo(channelId: Long, size: ImageSize): Resource
    fun getHeader(channelId: Long, size: ImageSize): Resource
    fun editChannel(channel: ChannelEditingModel): Channel
    fun removeChannel(userId: Long, channelId: Long)
    fun existsByTitle(channelId: Long? = null, title: String): Boolean
    fun existsByAlias(channelId: Long? = null, alias: String): Boolean
    fun createChannel(channel: ChannelCreationModel): Channel
}