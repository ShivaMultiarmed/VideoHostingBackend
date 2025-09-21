package mikhail.shell.video.hosting.dto

import mikhail.shell.video.hosting.domain.Channel
import mikhail.shell.video.hosting.domain.ChannelWithUser
import mikhail.shell.video.hosting.domain.Subscription

data class ChannelDto(
    val channelId: Long? = null,
    val ownerId: Long,
    val title: String,
    val alias: String? = null,
    val description: String? = null,
    val subscribers: Long = 0
)

fun ChannelDto.toDomain() = Channel(
    channelId = channelId,
    ownerId = ownerId,
    title = title,
    alias = alias,
    description = description,
    subscribers = subscribers
)

fun Channel.toDto() = ChannelDto(
    channelId = channelId,
    ownerId = ownerId,
    title = title,
    alias = alias,
    description = description,
    subscribers = subscribers
)

data class ChannelWithUserDto(
    val channelId: Long? = null,
    val ownerId: Long,
    val title: String,
    val alias: String? = null,
    val description: String? = null,
    val subscribers: Long = 0,
    val subscription: Subscription = Subscription.NOT_SUBSCRIBED
)

fun ChannelWithUser.toDto() = ChannelWithUserDto(
    channelId = channelId,
    ownerId = ownerId,
    title = title,
    alias = alias,
    description = description,
    subscribers = subscribers,
    subscription = subscription
)
