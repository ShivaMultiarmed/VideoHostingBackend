package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.Channel
import mikhail.shell.video.hosting.domain.ChannelWithUser
import mikhail.shell.video.hosting.domain.SubscriptionState
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

    override fun createChannel(channel: Channel): Channel {
        return channelRepository.save(channel.toEntity()).toDomain()
    }

    override fun getChannelsByOwnerId(userId: Long): List<Channel> {
        return channelRepository.findByOwnerId(userId).map { it.toDomain() }
    }
}