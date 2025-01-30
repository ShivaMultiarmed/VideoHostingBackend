package mikhail.shell.video.hosting.service

import com.google.firebase.messaging.FirebaseMessaging
import jakarta.transaction.Transactional
import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.domain.ApplicationPaths.CHANNEL_COVERS_BASE_PATH
import mikhail.shell.video.hosting.domain.ApplicationPaths.CHANNEL_AVATARS_BASE_PATH
import mikhail.shell.video.hosting.domain.SubscriptionState.NOT_SUBSCRIBED
import mikhail.shell.video.hosting.domain.SubscriptionState.SUBSCRIBED
import mikhail.shell.video.hosting.errors.ChannelCreationError
import mikhail.shell.video.hosting.errors.ChannelCreationError.EXISTS
import mikhail.shell.video.hosting.errors.CompoundError
import mikhail.shell.video.hosting.errors.HostingDataException
import mikhail.shell.video.hosting.repository.models.SubscriberId
import mikhail.shell.video.hosting.repository.ChannelRepository
import mikhail.shell.video.hosting.repository.SubscriberRepository
import mikhail.shell.video.hosting.repository.models.Subscriber
import mikhail.shell.video.hosting.repository.models.toDomain
import mikhail.shell.video.hosting.repository.models.toEntity
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ChannelServiceImpl @Autowired constructor(
    private val channelRepository: ChannelRepository,
    private val subscriberRepository: SubscriberRepository,
    private val fcm: FirebaseMessaging
) : ChannelService {

    private val CHANNELS_TOPICS_PREFIX = "channels"

    override fun provideChannelInfo(
        channelId: Long
    ): Channel {
        return channelRepository.findById(channelId).orElseThrow().toDomain()
    }

    override fun provideChannelForUser(channelId: Long, userId: Long): ChannelWithUser {
        val channel = channelRepository.findById(channelId).orElseThrow()
        val subscription = if (subscriberRepository.existsById(SubscriberId(channelId, userId)))
            SubscriptionState.SUBSCRIBED else NOT_SUBSCRIBED
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

    override fun checkIfSubscribed(channelId: Long, userId: Long): Boolean {
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

    override fun getChannelsBySubscriberId(userId: Long): List<Channel> {
        val subscriptions = subscriberRepository.findById_UserId(userId)
        val channelIds = subscriptions.map { it.id.channelId }
        return channelRepository.findAllById(channelIds).map { it.toDomain() }
    }

    override fun changeSubscriptionState(
        subscriberId: Long,
        channelId: Long,
        token: String,
        subscriptionState: SubscriptionState
    ): ChannelWithUser {
        val channelEntity = channelRepository.findById(channelId).orElseThrow()
        if (checkIfSubscribed(channelId, subscriberId) && subscriptionState == NOT_SUBSCRIBED) {
            channelRepository.save(
                channelEntity.copy(
                    subscribers = channelEntity.subscribers - 1
                )
            )
            subscriberRepository.deleteById(SubscriberId(channelId, subscriberId))
        } else if (!checkIfSubscribed(channelId, subscriberId) && subscriptionState == SUBSCRIBED) {
            subscriberRepository.save(Subscriber(SubscriberId(channelId, subscriberId)))
            channelRepository.save(
                channelEntity.copy(
                    subscribers = channelEntity.subscribers + 1
                )
            )
        }
        val newSubscriptionState = if (checkIfSubscribed(channelId, subscriberId)) SUBSCRIBED else NOT_SUBSCRIBED
        if (newSubscriptionState == SUBSCRIBED) {
            fcm.subscribeToTopic(listOf(token), "$CHANNELS_TOPICS_PREFIX.$channelId")
        } else {
            fcm.unsubscribeFromTopic(listOf(token), "$CHANNELS_TOPICS_PREFIX.$channelId")
        }
        val channel = channelRepository.findById(channelId).orElseThrow().toDomain()
        return ChannelWithUser(
            channelId,
            channel.ownerId,
            channel.title,
            channel.alias,
            channel.description,
            channel.subscribers,
            newSubscriptionState
        )
    }

    override fun resubscribe(userId: Long, token: String) {
        subscriberRepository.findById_UserId(userId).map { it.id.channelId }.forEach { fcm.subscribeToTopic(listOf(token), "$CHANNELS_TOPICS_PREFIX.$it") }
    }
}
