package mikhail.shell.video.hosting.service

import jakarta.transaction.Transactional
import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.domain.ApplicationPaths.CHANNEL_COVERS_BASE_PATH
import mikhail.shell.video.hosting.domain.ApplicationPaths.CHANNEL_AVATARS_BASE_PATH
import mikhail.shell.video.hosting.errors.ChannelCreationError
import mikhail.shell.video.hosting.errors.ChannelCreationError.EXISTS
import mikhail.shell.video.hosting.errors.CompoundError
import mikhail.shell.video.hosting.errors.HostingDataException
import mikhail.shell.video.hosting.repository.models.SubscriberId
import mikhail.shell.video.hosting.repository.ChannelRepository
import mikhail.shell.video.hosting.repository.SubscriberRepository
import mikhail.shell.video.hosting.repository.models.toDomain
import mikhail.shell.video.hosting.repository.models.toEntity
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ChannelServiceImpl @Autowired constructor(
    private val channelRepository: ChannelRepository,
    private val subscriberRepository: SubscriberRepository
) : ChannelService {

    override fun provideChannelInfo(
        channelId: Long
    ): Channel {
        return channelRepository.findById(channelId).orElseThrow().toDomain()
    }

    override fun provideChannelForUser(channelId: Long, userId: Long): ChannelWithUser {
        val channel = channelRepository.findById(channelId).orElseThrow()
        val subscription = if (subscriberRepository.existsById(SubscriberId(channelId, userId.toString())))
            SubscriptionState.SUBSCRIBED else SubscriptionState.NOT_SUBSCRIBED
        return ChannelWithUser(
            channel.channelId,
            channel.ownerId,
            channel.title,
            channel.alias,
            channel.description,
            channel.subscribers,
            subscription
        )
    }

    override fun checkIfSubscribed(channelId: Long, userId: String): Boolean {
        val id = SubscriberId(channelId, userId)
        return subscriberRepository.existsById(id)
    }

    @Transactional
    override fun createChannel(channel: Channel, avatar: File?, cover: File?): Channel {
        val error = CompoundError<ChannelCreationError>()
        if (channelRepository.existsByTitle(channel.title))
            error.add(EXISTS)
        if (error.isNotNull())
            throw HostingDataException(error)
        val createdChannel = channelRepository.save(channel.toEntity()).toDomain()
        val coverExtension = cover?.name?.parseExtension()
        cover?.content?.let {
            java.io.File("$CHANNEL_COVERS_BASE_PATH/${createdChannel.channelId}.$coverExtension").writeBytes(it)
        }
        val avatarExtension = cover?.name?.parseExtension()
        avatar?.content?.let {
            java.io.File("$CHANNEL_AVATARS_BASE_PATH/${createdChannel.channelId}.$avatarExtension").writeBytes(it)
        }
        return createdChannel
    }

    override fun getChannelsByOwnerId(userId: Long): List<Channel> {
        return channelRepository.findByOwnerId(userId).map { it.toDomain() }
    }
}
