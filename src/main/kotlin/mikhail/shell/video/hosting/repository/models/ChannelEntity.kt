package mikhail.shell.video.hosting.repository.models

import jakarta.persistence.*
import mikhail.shell.video.hosting.domain.Channel

@Entity
@Table(name = "channels")
data class ChannelEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "channel_id") val channelId: Long,
    val ownerId: String,
    val title: String,
    val alias: String,
    val description: String,
    val subscribers: Long
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
//
//@Entity
//@Table(name = "channels")
//data class ChannelWithUserEntity(
//    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "channel_id") val channelId: Long,
//    val ownerId: String,
//    val title: String,
//    val alias: String,
//    val description: String,
//    val subscribers: Long,
//    val subscription: Boolean
//)