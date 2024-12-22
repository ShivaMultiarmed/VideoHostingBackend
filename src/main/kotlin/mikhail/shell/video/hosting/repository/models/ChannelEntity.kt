package mikhail.shell.video.hosting.repository.models

import jakarta.persistence.*
import mikhail.shell.video.hosting.domain.ChannelInfo

@Entity
@Table(name = "channels")
data class ChannelEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val channelId: Long,
    val ownerId: Long,
    val title: String,
    val alias: String,
    val description: String,
    val subscribers: Long
)

fun ChannelEntity.toDomain() = ChannelInfo(channelId, ownerId, title, alias, description, subscribers)
fun ChannelInfo.toEntity() = ChannelEntity(channelId, ownerId, title, alias, description, subscribers)