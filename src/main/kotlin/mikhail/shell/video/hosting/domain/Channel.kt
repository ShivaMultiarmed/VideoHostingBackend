package mikhail.shell.video.hosting.domain

data class Channel(
    val channelId: Long,
    val ownerId: Long,
    val title: String,
    val alias: String?,
    val description: String?,
    val subscribers: Long
)

data class ChannelWithUser(
    val channelId: Long,
    val ownerId: Long,
    val title: String,
    val alias: String?,
    val description: String?,
    val subscribers: Long,
    val subscription: Subscription
)

infix fun Channel.with(subscription: Subscription) = ChannelWithUser(
    channelId = channelId,
    ownerId = ownerId,
    title = title,
    alias = alias,
    description = description,
    subscribers = subscribers,
    subscription = subscription
)

data class ChannelCreationModel(
    val ownerId: Long,
    val title: String,
    val alias: String?,
    val description: String?,
    val header: UploadedFile?,
    val logo: UploadedFile?
)

data class ChannelEditingModel(
    val channelId: Long,
    val ownerId: Long,
    val title: String,
    val alias: String?,
    val description: String?,
    val header: EditingAction<UploadedFile>,
    val logo: EditingAction<UploadedFile>
)