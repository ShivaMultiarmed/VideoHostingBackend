package mikhail.shell.video.hosting.domain

data class Channel(
    val channelId: Long,
    val ownerId: String,
    val title: String,
    val alias: String,
    val description: String,
    val subscribers: Long
)
