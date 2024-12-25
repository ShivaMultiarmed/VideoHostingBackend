package mikhail.shell.video.hosting.dto

import mikhail.shell.video.hosting.domain.Channel
import mikhail.shell.video.hosting.domain.ChannelWithUser
import mikhail.shell.video.hosting.domain.SubscriptionState

data class ChannelDto(
    val channelId: Long,
    val ownerId: String,
    val title: String,
    val alias: String,
    val description: String,
    val subscribers: Long,
    val coverUrl: String,
    val avatarUrl: String
)


fun Channel.toDto(
    coverUrl: String,
    avatarUrl: String
) = ChannelDto(
    channelId,
    ownerId,
    title,
    alias,
    description,
    subscribers,
    coverUrl,
    avatarUrl
)

data class ChannelWithUserDto(
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

fun ChannelWithUser.toDto(
    coverUrl: String,
    avatarUrl: String
) = ChannelWithUserDto(
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
