package mikhail.shell.video.hosting.service

import com.google.firebase.messaging.FirebaseMessaging
import jakarta.transaction.Transactional
import mikhail.shell.video.hosting.domain.*
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
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.util.MimeType
import org.springframework.util.MimeTypeUtils
import java.io.File
import java.net.URLConnection
import javax.activation.MimetypesFileTypeMap
import kotlin.io.path.*

@Service
class ChannelServiceWithDB @Autowired constructor(
    private val channelRepository: ChannelRepository,
    private val userRepository: UserRepository,
    private val subscriberRepository: SubscriberRepository,
    private val videoRepository: VideoRepository,
    private val videoSearchRepository: VideoSearchRepository,
    private val fcm: FirebaseMessaging,
    private val appPaths: ApplicationPathsInitializer,
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
        val errors = mutableMapOf<String, Error>()
        if (channelRepository.existsByTitle(channel.title)) {
            errors["titleError"] = TextError.EXISTS
        }
        if (channel.alias != null && channelRepository.existsByAlias(channel.alias)) {
            errors["aliasError"] = TextError.EXISTS
        }
        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }
        val createdChannel = channelRepository.save(channel.toEntity()).toDomain()
        val channelPath = Path(appPaths.CHANNELS_BASE_PATH, createdChannel.channelId!!.toString())
        if (channelPath.notExists()) {
            channelPath.createDirectory()
        }
        header?.let {
            val headerPath = channelPath.resolve("header")
            if (headerPath.notExists()) {
                headerPath.createDirectory()
            }
            uploadImage(
                uploadedFile = it,
                targetFile = "$headerPath/large.png",
                width = 1800,
                height = 200
            )
            uploadImage(
                uploadedFile = it,
                targetFile = "$headerPath/medium.png",
                width = 1000,
                height = 120
            )
            uploadImage(
                uploadedFile = it,
                targetFile = "$headerPath/small.png",
                width = 350,
                height = 60
            )
        }
        logo?.let {
            val logoPath = channelPath.resolve("logo")
            if (logoPath.notExists()) {
                logoPath.createDirectory()
            }
            uploadImage(
                uploadedFile = it.copy(),
                targetFile = "$logoPath/small.png",
                width = 64,
                height = 64
            )
            uploadImage(
                uploadedFile = it.copy(),
                targetFile = "$logoPath/medium.png",
                width = 128,
                height = 128
            )
            uploadImage(
                uploadedFile = it.copy(),
                targetFile = "$logoPath/large.png",
                width = 512,
                height = 512
            )
        }
        return createdChannel
    }

    override fun getLogo(channelId: Long, size: ImageSize): Resource {
        val file =
            Path(appPaths.CHANNELS_BASE_PATH, channelId.toString(), "logo", size.name.lowercase() + ".png").toFile()
        if (!channelRepository.existsById(channelId) || !file.exists()) {
            throw NoSuchElementException()
        } else {
            return FileSystemResource(file)
        }
    }

    override fun getChannelsByOwnerId(userId: Long): List<Channel> {
        return channelRepository.findByOwnerId(userId).map { it.toDomain() }
    }

    override fun getHeader(channelId: Long, size: ImageSize): Resource {
        val file =
            Path(appPaths.CHANNELS_BASE_PATH, channelId.toString(), "header", size.name.lowercase() + ".png").toFile()
        if (!channelRepository.existsById(channelId) || !file.exists()) {
            throw NoSuchElementException()
        } else {
            return FileSystemResource(file)
        }
    }

    override fun getChannelsByOwnerId(
        userId: Long,
        partIndex: Long,
        partSize: Int,
    ): List<Channel> {
        if (!userRepository.existsById(userId)) {
            throw NoSuchElementException()
        }
        return channelRepository.findByOwnerId(
            ownerId = userId,
            pageable = PageRequest.of(partIndex.toInt(), partSize)
        ).map { it.toDomain() }
    }

    override fun getSubscriptions(
        userId: Long,
        partIndex: Long,
        partSize: Int,
    ): List<Channel> {
        if (!userRepository.existsById(userId)) {
            throw NoSuchElementException()
        }
        val channelIds = subscriberRepository.findById_UserId(
            userId = userId,
            pageable = PageRequest.of(partIndex.toInt(), partSize)
        ).map { it.id.channelId }
        return channelRepository.findAllById(channelIds).map { it.toDomain() }
    }

    override fun changeSubscriptionState(
        subscriberId: Long,
        channelId: Long,
        subscription: Subscription,
        token: String,
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

    @OptIn(ExperimentalPathApi::class)
    override fun editChannel(
        channel: Channel,
        header: UploadedFile?,
        headerAction: EditAction,
        logo: UploadedFile?,
        logoAction: EditAction,
    ): Channel {
        val currentChannelEntity = channelRepository.findById(channel.channelId!!).orElseThrow()
        if (!channelRepository.existsByOwnerIdAndChannelId(channel.ownerId, channel.channelId)) {
            throw IllegalAccessException()
        }
        val errors = mutableMapOf<String, Error>()
        if (channelRepository.existsByTitle(channel.title) && currentChannelEntity.title != channel.title) {
            errors["titleError"] = TextError.EXISTS
        }
        if (channel.alias != null && channelRepository.existsByAlias(channel.alias) && currentChannelEntity.alias != channel.alias) {
            errors["aliasError"] = TextError.EXISTS
        }
        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }
        val editedChannel = channelRepository.save(
            currentChannelEntity.copy(
                title = channel.title,
                alias = channel.alias,
                description = channel.description
            )
        ).toDomain()
        val channelPath = Path(appPaths.CHANNELS_BASE_PATH, editedChannel.channelId!!.toString())
        if (channelPath.notExists()) {
            channelPath.createDirectory()
        }
        val headerPath = channelPath.resolve("header")
        if (headerAction == EditAction.REMOVE) {
            headerPath.deleteRecursively()
        } else if (headerAction == EditAction.UPDATE) {
            header?.let {
                if (headerPath.notExists()) {
                    headerPath.createDirectory()
                }
                uploadImage(
                    uploadedFile = it,
                    targetFile = "$headerPath/large.png",
                    width = 1800,
                    height = 200
                )
                uploadImage(
                    uploadedFile = it,
                    targetFile = "$headerPath/medium.png",
                    width = 1000,
                    height = 120
                )
                uploadImage(
                    uploadedFile = it,
                    targetFile = "$headerPath/small.png",
                    width = 350,
                    height = 60
                )
            }
        }
        val logoPath = channelPath.resolve("logo")
        if (logoAction == EditAction.REMOVE) {
            logoPath.deleteRecursively()
        } else if (logoAction == EditAction.UPDATE) {
            logo?.let {
                if (logoPath.notExists()) {
                    logoPath.createDirectory()
                }
                uploadImage(
                    uploadedFile = it.copy(),
                    targetFile = "$logoPath/small.png",
                    width = 64,
                    height = 64
                )
                uploadImage(
                    uploadedFile = it.copy(),
                    targetFile = "$logoPath/medium.png",
                    width = 128,
                    height = 128
                )
                uploadImage(
                    uploadedFile = it.copy(),
                    targetFile = "$logoPath/large.png",
                    width = 512,
                    height = 512
                )
            }
        }
        return editedChannel
    }

    override fun removeChannel(userId: Long, channelId: Long) {
        if (!channelRepository.existsById(channelId)) {
            throw NoSuchElementException()
        } else if (channelRepository.existsByOwnerIdAndChannelId(userId, channelId)) {
            throw IllegalAccessException()
        } else {
            findFileByName(File(appPaths.CHANNEL_LOGOS_BASE_PATH), channelId.toString())?.delete()
            findFileByName(File(appPaths.CHANNEL_HEADERS_BASE_PATH), channelId.toString())?.delete()
            val videoIds = videoRepository.findByChannelId(channelId).map { it.videoId }
            videoSearchRepository.deleteAllById(videoIds)
            videoIds.filterNotNull().forEach {
                findFileByName(File(appPaths.VIDEOS_COVERS_BASE_PATH), it.toString())?.delete()
                findFileByName(File(appPaths.VIDEOS_SOURCES_BASE_PATH), it.toString())?.delete()
            }
            channelRepository.deleteById(channelId)
        }
    }

    override fun existsByTitle(channelId: Long?, title: String): Boolean {
        return if (channelId == null) {
            channelRepository.existsByTitle(title)
        } else {
            channelRepository.existsByChannelIdAndTitle(channelId, title)
        }
    }

    override fun existsByAlias(channelId: Long?, alias: String): Boolean {
        return if (channelId == null) {
            channelRepository.existsByAlias(alias)
        } else {
            channelRepository.existsByChannelIdAndAlias(channelId, alias)
        }
    }

    override fun checkOwner(userId: Long, channelId: Long): Boolean {
        return channelRepository.existsByOwnerIdAndChannelId(userId, channelId)
    }

    override fun existsById(channelId: Long): Boolean {
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
