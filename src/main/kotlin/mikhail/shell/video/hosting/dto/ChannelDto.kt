package mikhail.shell.video.hosting.dto

import mikhail.shell.video.hosting.domain.Channel
import mikhail.shell.video.hosting.domain.ChannelWithUser
import mikhail.shell.video.hosting.domain.SubscriptionState

data class ChannelDto(
    val channelId: Long? = null,
    val ownerId: Long,
    val title: String,
    val alias: String? = null,
    val description: String? = null,
    val subscribers: Long = 0,
    val coverUrl: String? = null,
    val avatarUrl: String? = null
)

fun ChannelDto.toDomain() = Channel(
    channelId = channelId,
    ownerId = ownerId,
    title = title,
    alias = alias,
    description = description,
    subscribers = subscribers
)

fun Channel.toDto(
    coverUrl: String? = null,
    avatarUrl: String? = null
) = ChannelDto(
    channelId = channelId,
    ownerId = ownerId,
    title = title,
    alias = alias,
    description = description,
    subscribers = subscribers,
    coverUrl = coverUrl,
    avatarUrl = avatarUrl
)

data class ChannelWithUserDto(
    val channelId: Long? = null,
    val ownerId: Long,
    val title: String,
    val alias: String? = null,
    val description: String? = null,
    val subscribers: Long = 0,
    val subscription: SubscriptionState = SubscriptionState.NOT_SUBSCRIBED,
    val coverUrl: String? = null,
    val avatarUrl: String? = null
)

fun ChannelWithUser.toDto(
    coverUrl: String? = null,
    avatarUrl: String? = null
) = ChannelWithUserDto(
    channelId = channelId,
    ownerId = ownerId,
    title = title,
    alias = alias,
    description = description,
    subscribers = subscribers,
    subscription = subscription,
    coverUrl = coverUrl,
    avatarUrl = avatarUrl
)
