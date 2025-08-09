package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.*

interface ChannelService {
    fun provideChannelInfo(channelId: Long): Channel
    fun provideChannelForUser(channelId: Long, userId: Long): ChannelWithUser
    fun checkIfSubscribed(channelId: Long, userId: Long): Boolean
    fun createChannel(
        channel: Channel,
        avatar: File?,
        cover: File?
    ): Channel
    fun getChannelsByOwnerId(userId: Long): List<Channel>
    fun getChannelsBySubscriberId(userId: Long): List<Channel>
    fun changeSubscriptionState(
        subscriberId: Long,
        channelId: Long,
        token: String
    )
    fun subscribeToNotifications(userId: Long, token: String)
    fun unsubscribeFromNotifications(userId: Long, token: String)
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
    fun checkExistsence(channelId: Long): Boolean
}