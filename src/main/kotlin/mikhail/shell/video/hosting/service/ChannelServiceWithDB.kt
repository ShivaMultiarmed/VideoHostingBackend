package mikhail.shell.video.hosting.service

import com.google.firebase.messaging.FirebaseMessaging
import jakarta.transaction.Transactional
import jakarta.validation.ConstraintViolationException
import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.domain.ApplicationPaths.CHANNEL_HEADERS_BASE_PATH
import mikhail.shell.video.hosting.domain.ApplicationPaths.CHANNEL_LOGOS_BASE_PATH
import mikhail.shell.video.hosting.domain.ApplicationPaths.VIDEOS_COVERS_BASE_PATH
import mikhail.shell.video.hosting.domain.ApplicationPaths.VIDEOS_PLAYABLES_BASE_PATH
import mikhail.shell.video.hosting.domain.Subscription.NOT_SUBSCRIBED
import mikhail.shell.video.hosting.domain.Subscription.SUBSCRIBED
import mikhail.shell.video.hosting.elastic.repository.VideoSearchRepository
import mikhail.shell.video.hosting.entities.*
import mikhail.shell.video.hosting.repository.ChannelRepository
import mikhail.shell.video.hosting.repository.SubscriberRepository
import mikhail.shell.video.hosting.repository.UserRepository
import mikhail.shell.video.hosting.repository.VideoRepository
import mikhail.shell.video.hosting.errors.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.security.core.context.SecurityContextHolder
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

    override fun get(channelId: Long): Channel {
        return channelRepository.findById(channelId).orElseThrow().toDomain()
    }

    override fun getForUser(channelId: Long, userId: Long): ChannelWithUser {
        val channel = channelRepository.findById(channelId).orElseThrow().toDomain()
        if (!userRepository.existsById(userId)) {
            throw IllegalArgumentException()
        }
        val subscription =
            if (subscriberRepository.existsById(SubscriberId(channelId, userId))) SUBSCRIBED else NOT_SUBSCRIBED
        return channel with subscription
    }

    override fun getChannel(channelId: Long): Channel {
        return channelRepository.findById(channelId).orElseThrow().toDomain()
    }

    override fun checkIfSubscribed(channelId: Long, userId: Long): Boolean {
        return subscriberRepository.existsById(SubscriberId(channelId, userId))
    }

    @Transactional
    override fun createChannel(channel: Channel, logo: UploadedFile?, header: UploadedFile?): Channel {
        val createdChannel = try {
            channelRepository.save(channel.toEntity()).toDomain()
        } catch (e: ConstraintViolationException) {
            throw UniquenessViolationException()
        }
        header?.let {
            uploadImage(
                uploadedFile = it,
                targetFile = "$CHANNEL_HEADERS_BASE_PATH/${createdChannel.channelId}.jpg",
                width = 512,
                height = 128
            )
        }
        logo?.let {
            uploadImage(
                uploadedFile = it,
                targetFile = "$CHANNEL_LOGOS_BASE_PATH/${createdChannel.channelId}.jpg",
                width = 128,
                height = 128
            )
        }
        return createdChannel
    }

    override fun getLogo(channelId: Long): Resource {
        return FileSystemResource(
            findFileByName(CHANNEL_LOGOS_BASE_PATH, channelId.toString())
            .takeUnless { !channelRepository.existsById(channelId) || it?.exists() != true }
            ?: throw NoSuchElementException()
        )
    }

    override fun getHeader(channelId: Long): Resource {
        return FileSystemResource(
            findFileByName(CHANNEL_HEADERS_BASE_PATH, channelId.toString())
            .takeUnless { !channelRepository.existsById(channelId) || it?.exists() != true }
            ?: throw NoSuchElementException()
        )
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
        val channelIds = subscriberRepository.findById_UserId(userId).map { it.id.channelId }
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
        header: UploadedFile?,
        headerAction: EditAction,
        logo: UploadedFile?,
        logoAction: EditAction
    ): Channel {
        val currentChannelEntity = channelRepository
            .findById(channel.channelId!!)
            .orElseThrow()
        if (!channelRepository.existsByOwnerIdAndChannelId(channel.ownerId, channel.channelId)) {
            throw IllegalAccessException()
        }
        if (channelRepository.existsByTitle(channel.title)
            && currentChannelEntity.title != channel.title ||
            channel.alias != null
            && channelRepository.existsByAlias(channel.alias)
            && currentChannelEntity.alias != channel.alias
        ) {
            throw UniquenessViolationException()
        }
        val editedChannel = channelRepository.save(
            currentChannelEntity.copy(
                title = channel.title,
                alias = channel.alias,
                description = channel.description
            )
        ).toDomain()
        if (headerAction != EditAction.KEEP) {
            findFileByName(CHANNEL_HEADERS_BASE_PATH, channel.channelId.toString())?.delete()
        }
        if (headerAction == EditAction.UPDATE) {
            header?.let { uploadedFile ->
                uploadImage(
                    uploadedFile = uploadedFile,
                    targetFile = "$CHANNEL_HEADERS_BASE_PATH/${editedChannel.channelId}.jpg",
                    width = 512,
                    height = 128
                )
            }
        }
        if (logoAction != EditAction.KEEP) {
            findFileByName(CHANNEL_LOGOS_BASE_PATH, channel.channelId.toString())?.delete()
        }
        if (logoAction == EditAction.UPDATE) {
            logo?.let { uploadedFile ->
                uploadImage(
                    uploadedFile = uploadedFile,
                    targetFile = "$CHANNEL_LOGOS_BASE_PATH/${editedChannel.channelId}.jpg",
                    width = 512,
                    height = 128
                )
            }
        }
        return editedChannel
    }

    override fun removeChannel(channelId: Long) {
        val userId = SecurityContextHolder.getContext().authentication.principal as? Long
            ?: throw UnauthenticatedException()
        if (!channelRepository.existsById(channelId)) {
            throw NoSuchElementException()
        } else if (channelRepository.existsByOwnerIdAndChannelId(userId, channelId)) {
            throw IllegalAccessException()
        } else {
            findFileByName(File(CHANNEL_LOGOS_BASE_PATH), channelId.toString())?.delete()
            findFileByName(File(CHANNEL_HEADERS_BASE_PATH), channelId.toString())?.delete()
            val videoIds = videoRepository.findByChannelId(channelId).map { it.videoId }
            videoSearchRepository.deleteAllById(videoIds)
            videoIds.filterNotNull().forEach {
                findFileByName(File(VIDEOS_COVERS_BASE_PATH), it.toString())?.delete()
                findFileByName(File(VIDEOS_PLAYABLES_BASE_PATH), it.toString())?.delete()
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
