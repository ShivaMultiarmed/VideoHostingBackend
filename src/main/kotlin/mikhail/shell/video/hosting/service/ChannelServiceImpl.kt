package mikhail.shell.video.hosting.service

import com.google.firebase.messaging.FirebaseMessaging
import jakarta.transaction.Transactional
import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.domain.ApplicationPaths.CHANNEL_COVERS_BASE_PATH
import mikhail.shell.video.hosting.domain.ApplicationPaths.CHANNEL_AVATARS_BASE_PATH
import mikhail.shell.video.hosting.domain.ApplicationPaths.VIDEOS_COVERS_BASE_PATH
import mikhail.shell.video.hosting.domain.ApplicationPaths.VIDEOS_PLAYABLES_BASE_PATH
import mikhail.shell.video.hosting.domain.SubscriptionState.NOT_SUBSCRIBED
import mikhail.shell.video.hosting.domain.SubscriptionState.SUBSCRIBED
import mikhail.shell.video.hosting.elastic.repository.VideoSearchRepository
import mikhail.shell.video.hosting.errors.ChannelCreationError
import mikhail.shell.video.hosting.errors.ChannelCreationError.EXISTS
import mikhail.shell.video.hosting.errors.CompoundError
import mikhail.shell.video.hosting.errors.EditChannelError
import mikhail.shell.video.hosting.errors.HostingDataException
import mikhail.shell.video.hosting.repository.entities.SubscriberId
import mikhail.shell.video.hosting.repository.ChannelRepository
import mikhail.shell.video.hosting.repository.SubscriberRepository
import mikhail.shell.video.hosting.repository.VideoRepository
import mikhail.shell.video.hosting.repository.entities.Subscriber
import mikhail.shell.video.hosting.repository.entities.toDomain
import mikhail.shell.video.hosting.repository.entities.toEntity
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ChannelServiceImpl @Autowired constructor(
    private val channelRepository: ChannelRepository,
    private val subscriberRepository: SubscriberRepository,
    private val videoRepository: VideoRepository,
    private val videoSearchRepository: VideoSearchRepository,
    private val fcm: FirebaseMessaging
) : ChannelService {

    private val MAX_BUFFER = 10 * 1024 * 1024
    private val MAX_FILE_SIZE = 10 * 1024 * 1024

    private val CHANNELS_TOPICS_PREFIX = "channels"

    override fun provideChannelInfo(
        channelId: Long
    ): Channel {
        return channelRepository.findById(channelId).orElseThrow().toDomain()
    }

    override fun provideChannelForUser(channelId: Long, userId: Long): ChannelWithUser {
        val channel = channelRepository.findById(channelId).orElseThrow()
        val subscription = if (subscriberRepository.existsById(SubscriberId(channelId, userId)))
            SUBSCRIBED else NOT_SUBSCRIBED
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

    override fun getChannel(channelId: Long): Channel {
        val channelEntity = channelRepository.findById(channelId).orElseThrow()
        return channelEntity.toDomain()
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

    override fun editChannel(
        channel: Channel,
        editCoverAction: EditAction,
        coverFile: File?,
        editAvatarAction: EditAction,
        avatarFile: File?
    ): Channel {
        val error = CompoundError<EditChannelError>()
        if (channel.title.length > 255) {
            error.add(EditChannelError.TITLE_TOO_LARGE)
        }
        if ((channel.alias?.length ?: 0) > 255) {
            error.add(EditChannelError.ALIAS_TOO_LARGE)
        }
        if ((channel.description?.length ?: 0) > 5000) {
            error.add(EditChannelError.DESCRIPTION_TOO_LARGE)
        }
        coverFile?.let {
            if ((it.content?.size ?: 0) > MAX_FILE_SIZE) {
                error.add(EditChannelError.COVER_TOO_LARGE)
            }
        }
        avatarFile?.let {
            if ((it.content?.size ?: 0) > MAX_FILE_SIZE) {
                error.add(EditChannelError.AVATAR_TOO_LARGE)
            }
        }
        if (error.isNotNull()) {
            throw HostingDataException(error)
        }
        val editedChannel = channelRepository.save(channel.toEntity()).toDomain()
        when (editCoverAction) {
            EditAction.KEEP -> Unit
            EditAction.REMOVE -> {
                findFileByName(
                    java.io.File(CHANNEL_COVERS_BASE_PATH),
                    channel.channelId!!.toString()
                )?.delete()
            }
            EditAction.UPDATE -> {
                coverFile?.let {
                    findFileByName(
                        java.io.File(CHANNEL_COVERS_BASE_PATH),
                        channel.channelId!!.toString()
                    )?.delete()
                    val extension = it.name!!.parseExtension()
                    val fileName = "${channel.channelId}.$extension"
                    java.io.File(CHANNEL_COVERS_BASE_PATH, fileName).writeBytes(coverFile.content!!)
                }
            }
        }
        when (editAvatarAction) {
            EditAction.KEEP -> Unit
            EditAction.REMOVE -> {
                findFileByName(
                    java.io.File(CHANNEL_AVATARS_BASE_PATH),
                    channel.channelId!!.toString()
                )?.delete()
            }
            EditAction.UPDATE -> {
                avatarFile?.let {
                    findFileByName(
                        java.io.File(CHANNEL_AVATARS_BASE_PATH),
                        channel.channelId!!.toString()
                    )?.delete()
                    val extension = it.name!!.parseExtension()
                    val fileName = "${channel.channelId}.$extension"
                    java.io.File(CHANNEL_AVATARS_BASE_PATH, fileName).writeBytes(avatarFile.content!!)
                }
            }
        }
        return editedChannel
    }

    override fun removeChannel(channelId: Long) {
        if (!channelRepository.existsById(channelId)) {
            throw NoSuchElementException()
        } else {
            findFileByName(
                java.io.File(CHANNEL_AVATARS_BASE_PATH),
                channelId.toString()
            )?.delete()
            findFileByName(
                java.io.File(CHANNEL_COVERS_BASE_PATH),
                channelId.toString()
            )?.delete()
            val videoIds = videoRepository.findByChannelId(channelId).map { it.videoId }
            videoSearchRepository.deleteAllById(videoIds)
            videoIds.filterNotNull().forEach {
                findFileByName(
                    java.io.File(VIDEOS_COVERS_BASE_PATH),
                    it.toString()
                )?.delete()
                findFileByName(
                    java.io.File(VIDEOS_PLAYABLES_BASE_PATH),
                    it.toString()
                )?.delete()
            }
            channelRepository.deleteById(channelId)
        }
    }

    override fun checkOwner(userId: Long, channelId: Long): Boolean {
        return channelRepository.existsByOwnerIdAndChannelId(userId, channelId)
    }

    override fun resubscribe(userId: Long, token: String) {
        subscriberRepository.findById_UserId(userId)
            .map { it.id.channelId }
            .forEach { fcm.subscribeToTopic(listOf(token), "$CHANNELS_TOPICS_PREFIX.$it") }
    }
}
