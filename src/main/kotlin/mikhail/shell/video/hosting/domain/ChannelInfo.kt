package mikhail.shell.video.hosting.domain

import jakarta.persistence.*

@Entity
@Table(name = "channels")
data class ChannelInfo(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val channelId: Long,
    val ownerId: Long,
    val title: String,
    val alias: String,
    val description: String,
    val subscribers: Long
)
