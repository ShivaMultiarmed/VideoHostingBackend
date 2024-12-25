package mikhail.shell.video.hosting.dto

import mikhail.shell.video.hosting.domain.ChannelInfo
import mikhail.shell.video.hosting.domain.SubscriptionState

data class ChannelDto(
    val channelId: Long,
    val ownerId: String,
    val title: String,
    val alias: String,
    val description: String,
    val subscribers: Long,
    val subscription: SubscriptionState,
    val coverUrl: String,
    val avatarUrl: String
)


fun ChannelInfo.toDto(
    subscription: SubscriptionState,
    coverUrl: String,
    avatarUrl: String
) = ChannelDto(
    channelId,
    ownerId,
    title,
    alias,
    description,
    subscribers,
    subscription,
    coverUrl,
    avatarUrl
)
