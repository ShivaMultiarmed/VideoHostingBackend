package mikhail.shell.video.hosting.domain

data class Channel(
    val channelId: Long? = null,
    val ownerId: Long,
    val title: String,
    val alias: String,
    val description: String,
    val subscribers: Long
)

data class ChannelWithUser(
    val channelId: Long? = null,
    val ownerId: Long,
    val title: String,
    val alias: String,
    val description: String,
    val subscribers: Long,
    val subscription: SubscriptionState
)

