package mikhail.shell.video.hosting.service

import com.google.firebase.messaging.FirebaseMessaging
import jakarta.transaction.Transactional
import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.domain.ApplicationPaths.CHANNEL_COVERS_BASE_PATH
import mikhail.shell.video.hosting.domain.ApplicationPaths.CHANNEL_AVATARS_BASE_PATH
import mikhail.shell.video.hosting.domain.ApplicationPaths.VIDEOS_COVERS_BASE_PATH
import mikhail.shell.video.hosting.domain.ApplicationPaths.VIDEOS_PLAYABLES_BASE_PATH
import mikhail.shell.video.hosting.domain.Subscription.NOT_SUBSCRIBED
import mikhail.shell.video.hosting.domain.Subscription.SUBSCRIBED
import mikhail.shell.video.hosting.elastic.repository.VideoSearchRepository
import mikhail.shell.video.hosting.errors.ChannelCreationError
import mikhail.shell.video.hosting.errors.ChannelCreationError.*
import mikhail.shell.video.hosting.errors.CompoundError
import mikhail.shell.video.hosting.errors.EditChannelError
import mikhail.shell.video.hosting.errors.ValidationException
import mikhail.shell.video.hosting.repository.entities.SubscriberId
import mikhail.shell.video.hosting.repository.ChannelRepository
import mikhail.shell.video.hosting.repository.SubscriberRepository
import mikhail.shell.video.hosting.repository.UserRepository
import mikhail.shell.video.hosting.repository.VideoRepository
import mikhail.shell.video.hosting.repository.entities.Subscriber
import mikhail.shell.video.hosting.repository.entities.toDomain
import mikhail.shell.video.hosting.repository.entities.toEntity
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File

@Service
class ChannelServiceWithDB @Autowired constructor(
    private val channelRepository: ChannelRepository,
    private val userRepository: UserRepository,
    private val subscriberRepository: SubscriberRepository,
    private val videoRepository: VideoRepository,
    private val videoSearchRepository: VideoSearchRepository,
    private val fcm: FirebaseMessaging
) : ChannelService {

    override fun provideChannelInfo(
        channelId: Long
    ): Channel {
        return channelRepository.findById(channelId).orElseThrow().toDomain()
    }

    override fun provideChannelForUser(channelId: Long, userId: Long): ChannelWithUser {
        val channel = channelRepository.findById(channelId).orElseThrow()
        if (!userRepository.existsById(userId)) {
            throw IllegalArgumentException()
        }
        val subscription = if (subscriberRepository.existsById(SubscriberId(channelId, userId))) SUBSCRIBED else NOT_SUBSCRIBED
        return channel.toDomain() with subscription
    }

    override fun getChannel(channelId: Long): Channel {
        return channelRepository.findById(channelId).orElseThrow().toDomain()
    }

    override fun checkIfSubscribed(channelId: Long, userId: Long): Boolean {
        return subscriberRepository.existsById(SubscriberId(channelId, userId))
    }

    @Transactional
    override fun createChannel(channel: Channel, avatar: UploadedFile?, cover: UploadedFile?): Channel {
        val compoundError = CompoundError<ChannelCreationError>()
        if (channelRepository.existsByTitle(channel.title)) {
            compoundError.add(TITLE_EXISTS)
        }
        if (channel.title.length > ValidationRules.MAX_TITLE_LENGTH) {
            compoundError.add(TITLE_TOO_LARGE)
        }
        if ((channel.alias?.length ?: 0) > ValidationRules.MAX_TITLE_LENGTH) {
            compoundError.add(ALIAS_TOO_LARGE)
        } else if (channel.alias != null && channelRepository.existsByAlias(channel.alias)) {
            compoundError.add(ALIAS_EXISTS)
        }
        if ((channel.description?.length ?: 0) > ValidationRules.MAX_TEXT_LENGTH) {
            compoundError.add(DESCRIPTION_TOO_LARGE)
        }
        if (compoundError.isNotEmpty()) {
            throw ValidationException(compoundError)
        }
        val channelEntityToCreate = channel
            .toEntity()
            .copy(
                channelId = null,
                subscribers = 0
            )
        val createdChannel = channelRepository.save(channelEntityToCreate).toDomain()
        cover?.let {
            uploadImage(
                uploadedFile = it,
                targetFile = "$CHANNEL_COVERS_BASE_PATH/${createdChannel.channelId}.jpg",
                width = 512,
                height = 128
            )
        }
        avatar?.let {
            uploadImage(
                uploadedFile = it,
                targetFile = "$CHANNEL_AVATARS_BASE_PATH/${createdChannel.channelId}.jpg",
                width = 128,
                height = 128
            )
        }
        return createdChannel
    }

    override fun getChannelsByOwnerId(userId: Long): List<Channel> {
        if (!userRepository.existsById(userId)) {
            throw NoSuchElementException()
        }
        return channelRepository.findByOwnerId(userId).map { it.toDomain() }
    }

    override fun getChannelsBySubscriberId(userId: Long): List<Channel> {
        if (!userRepository.existsById(userId)) {
            throw NoSuchElementException()
        }
        val subscriptions = subscriberRepository.findById_UserId(userId)
        val channelIds = subscriptions.map { it.id.channelId }
        return channelRepository.findAllById(channelIds).map { it.toDomain() }
    }

    override fun changeSubscriptionState(
        subscriberId: Long,
        channelId: Long,
        subscription: Subscription,
        token: String
    ): ChannelWithUser {
        val channelEntity = channelRepository.findById(channelId).orElseThrow()
        var newSubscribersNumber = channelEntity.subscribers
        if (checkIfSubscribed(channelId, subscriberId) && subscription == NOT_SUBSCRIBED) {
            subscriberRepository.deleteById(SubscriberId(channelId, subscriberId))
            newSubscribersNumber--
        }
        if (!checkIfSubscribed(channelId, subscriberId) && subscription == SUBSCRIBED) {
            subscriberRepository.save(Subscriber(SubscriberId(channelId, subscriberId)))
            newSubscribersNumber++
        }
        val savedChannel = channelRepository.save(
            channelEntity.copy(subscribers = newSubscribersNumber)
        )
        if (subscription == SUBSCRIBED) {
            fcm.subscribeToTopic(listOf(token), "${CHANNELS_TOPICS_PREFIX}.$channelId")
        } else {
            fcm.unsubscribeFromTopic(listOf(token), "${CHANNELS_TOPICS_PREFIX}.$channelId")
        }
        return savedChannel.toDomain() with subscription
    }

    override fun editChannel(
        channel: Channel,
        editCoverAction: EditAction,
        coverFile: UploadedFile?,
        editAvatarAction: EditAction,
        avatarFile: UploadedFile?
    ): Channel {
        val currentChannelEntity = channelRepository
            .findById(channel.channelId!!)
            .get()
        val compoundError = CompoundError<EditChannelError>()
        if (channel.title.length > ValidationRules.MAX_TITLE_LENGTH) {
            compoundError.add(EditChannelError.TITLE_TOO_LARGE)
        } else if (
            channelRepository.existsByTitle(channel.title)
            && currentChannelEntity.title != channel.title
        ) {
            compoundError.add(EditChannelError.TITLE_EXISTS)
        }
        if ((channel.alias?.length ?: 0) > ValidationRules.MAX_TITLE_LENGTH) {
            compoundError.add(EditChannelError.ALIAS_TOO_LARGE)
        } else if (
            channel.alias != null
            && channelRepository.existsByAlias(channel.alias)
            && currentChannelEntity.alias != channel.alias
        ) {
            compoundError.add(EditChannelError.ALIAS_EXISTS)
        }
        if ((channel.description?.length ?: 0) > ValidationRules.MAX_TEXT_LENGTH) {
            compoundError.add(EditChannelError.DESCRIPTION_TOO_LARGE)
        }
        if (compoundError.isNotEmpty()) {
            throw ValidationException(compoundError)
        }
        val editedChannel = channelRepository.save(channel.toEntity()).toDomain()
        when (editCoverAction) {
            EditAction.KEEP -> Unit
            EditAction.REMOVE -> {
                File(CHANNEL_COVERS_BASE_PATH, channel.channelId.toString()).delete()
            }
            EditAction.UPDATE -> {
                coverFile?.let { uploadedFile ->
                    File(CHANNEL_COVERS_BASE_PATH, channel.channelId.toString()).delete()
                    uploadImage(
                        uploadedFile = uploadedFile,
                        targetFile = "$CHANNEL_COVERS_BASE_PATH/${editedChannel.channelId}.jpg",
                        width = 512,
                        height = 128
                    )
                }
            }
        }
        when (editAvatarAction) {
            EditAction.KEEP -> Unit
            EditAction.REMOVE -> {
                File(CHANNEL_AVATARS_BASE_PATH, channel.channelId.toString()).delete()
            }

            EditAction.UPDATE -> {
                avatarFile?.let { uploadedFile ->
                    File(CHANNEL_AVATARS_BASE_PATH, channel.channelId.toString()).delete()
                    uploadImage(
                        uploadedFile = uploadedFile,
                        targetFile = "$CHANNEL_AVATARS_BASE_PATH/${editedChannel.channelId}.jpg",
                        width = 128,
                        height = 128
                    )
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

    override fun checkExistence(channelId: Long): Boolean {
        return channelRepository.existsById(channelId)
    }

    override fun subscribeToNotifications(userId: Long, token: String) {
        subscriberRepository.findById_UserId(userId)
            .map { it.id.channelId }
            .forEach { fcm.subscribeToTopic(listOf(token), "${CHANNELS_TOPICS_PREFIX}.$it") }
    }

    override fun unsubscribeFromNotifications(userId: Long, token: String) {
        subscriberRepository.findById_UserId(userId)
            .map { it.id.channelId }
            .forEach { fcm.unsubscribeFromTopic(listOf(token), "${CHANNELS_TOPICS_PREFIX}.$it") }
    }

    private companion object {
        const val CHANNELS_TOPICS_PREFIX = "channels"
    }
}
