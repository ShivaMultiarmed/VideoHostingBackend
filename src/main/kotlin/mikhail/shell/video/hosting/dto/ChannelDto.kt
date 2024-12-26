package mikhail.shell.video.hosting.dto

import mikhail.shell.video.hosting.domain.Channel
import mikhail.shell.video.hosting.domain.ChannelWithUser
import mikhail.shell.video.hosting.domain.SubscriptionState

data class ChannelDto(
    val channelId: Long? = null,
    val ownerId: Long,
    val title: String,
    val alias: String,
    val description: String,
    val subscribers: Long = 0,
    val coverUrl: String? = null,
    val avatarUrl: String? = null
)

fun ChannelDto.toDomain() = Channel(
    channelId,
    ownerId,
    title,
    alias,
    description,
    subscribers
)

fun Channel.toDto(
    coverUrl: String? = null,
    avatarUrl: String? = null
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
    val channelId: Long? = null,
    val ownerId: Long,
    val title: String,
    val alias: String,
    val description: String,
    val subscribers: Long,
    val subscription: SubscriptionState,
    val coverUrl: String? = null,
    val avatarUrl: String? = null
)

fun ChannelWithUser.toDto(
    coverUrl: String? = null,
    avatarUrl: String? = null
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
