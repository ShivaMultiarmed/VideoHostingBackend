package mikhail.shell.video.hosting.repository.entities

import jakarta.persistence.*
import mikhail.shell.video.hosting.domain.Channel

@Entity
@Table(name = "channels")
data class ChannelEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "channel_id")
    val channelId: Long? = null,
    @Column(name = "owner_id")
    val ownerId: Long,
    val title: String,
    val alias: String? = null,
    val description: String? = null,
    val subscribers: Long = 0
)

fun ChannelEntity.toDomain() = Channel(
    channelId = channelId,
    ownerId = ownerId,
    title = title,
    alias = alias,
    description = description,
    subscribers = subscribers
)
fun Channel.toEntity() = ChannelEntity(
    channelId = channelId,
    ownerId = ownerId,
    title = title,
    alias = alias,
    description = description,
    subscribers = subscribers
)