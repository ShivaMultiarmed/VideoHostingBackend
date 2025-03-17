package mikhail.shell.video.hosting.repository.models

import jakarta.persistence.*
import mikhail.shell.video.hosting.domain.Channel

@Entity
@Table(name = "channels")
data class ChannelEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "channel_id") val channelId: Long? = null,
    val ownerId: Long,
    val title: String,
    val alias: String? = null,
    val description: String? = null,
    val subscribers: Long = 0
)

fun ChannelEntity.toDomain() = Channel(
    channelId,
    ownerId,
    title,
    alias,
    description,
    subscribers
)
fun Channel.toEntity() = ChannelEntity(
    channelId,
    ownerId,
    title,
    alias,
    description,
    subscribers
)