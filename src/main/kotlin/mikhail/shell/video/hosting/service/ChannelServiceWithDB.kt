package mikhail.shell.video.hosting.service

import com.google.firebase.messaging.FirebaseMessaging
import jakarta.transaction.Transactional
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
    override fun createChannel(channel: ChannelCreationModel): Channel {
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
        val createdChannel = channelRepository.save(
            ChannelEntity(
                ownerId = channel.ownerId,
                title = channel.title,
                alias = channel.alias,
                description = channel.description
            )
        ).toDomain()
        val channelPath = Path(appPaths.CHANNELS_BASE_PATH, createdChannel.channelId.toString())
        if (channelPath.notExists()) {
            channelPath.createDirectory()
        }
        channel.header?.let {
            val headerPath = channelPath.resolve("header")
            if (headerPath.notExists()) {
                headerPath.createDirectory()
            }
            val ext = it.fileName.parseExtension()
            runBlocking {
                val smallImage = launch {
                    uploadImage(
                        uploadedFile = it,
                        targetFile = "$headerPath/small.$ext",
                        width = 600,
                        height = 100,
                        compress = true
                    )
                }
                val mediumImage = launch {
                    uploadImage(
                        uploadedFile = it,
                        targetFile = "$headerPath/medium.$ext",
                        width = 1200,
                        height = 200,
                        compress = true
                    )
                }
                val largeImage = launch {
                    uploadImage(
                        uploadedFile = it,
                        targetFile = "$headerPath/large.$ext",
                        width = 2400,
                        height = 400,
                        compress = true
                    )
                }
                setOf(smallImage, mediumImage, largeImage).joinAll()
            }
        }
        channel.logo?.let {
            val logoPath = channelPath.resolve("logo")
            if (logoPath.notExists()) {
                logoPath.createDirectory()
            }
            val ext = it.fileName.parseExtension()
            runBlocking {
                val smallImage = launch {
                    uploadImage(
                        uploadedFile = it,
                        targetFile = "$logoPath/small.$ext",
                        width = 64,
                        height = 64,
                        compress = true
                    )
                }
                val mediumImage = launch {
                    uploadImage(
                        uploadedFile = it,
                        targetFile = "$logoPath/medium.$ext",
                        width = 192,
                        height = 192,
                        compress = true
                    )
                }
                val largeImage = launch {
                    uploadImage(
                        uploadedFile = it,
                        targetFile = "$logoPath/large.$ext",
                        width = 512,
                        height = 512,
                        compress = true
                    )
                }
                setOf(smallImage, mediumImage, largeImage).joinAll()
            }
        }
        return createdChannel
    }

    override fun getLogo(channelId: Long, size: ImageSize): Resource {
        val directory = Path(appPaths.CHANNELS_BASE_PATH, channelId.toString(), "logo")
        val logo = findFileByName(directory, size.name.lowercase())
        if (!channelRepository.existsById(channelId) || logo == null) {
            throw NoSuchElementException()
        } else {
            return FileSystemResource(logo)
        }
    }

    override fun getChannelsByOwnerId(userId: Long): List<Channel> {
        return channelRepository.findByOwnerId(userId).map { it.toDomain() }
    }

    override fun getHeader(channelId: Long, size: ImageSize): Resource {
        val directory = Path(appPaths.CHANNELS_BASE_PATH, channelId.toString(), "header")
        val header = findFileByName(directory, size.name.lowercase())
        if (!channelRepository.existsById(channelId) || header == null) {
            throw NoSuchElementException()
        } else {
            return FileSystemResource(header)
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

    override fun subscribe(
        subscriberId: Long,
        channelId: Long,
        subscription: Subscription,
        token: String?,
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
        val topic = "channels/$channelId/subscribers"
        if (token != null) {
            if (subscription == SUBSCRIBED) {
                fcm.subscribeToTopic(listOf(token), topic)
            } else {
                fcm.unsubscribeFromTopic(listOf(token), topic)
            }
        }
        return savedChannel.toDomain() with subscription
    }

    @OptIn(ExperimentalPathApi::class)
    override fun editChannel(channel: ChannelEditingModel): Channel {
        val currentChannelEntity = channelRepository.findById(channel.channelId).orElseThrow()
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
        val channelPath = Path(appPaths.CHANNELS_BASE_PATH, editedChannel.channelId.toString())
        if (channelPath.notExists()) {
            channelPath.createDirectory()
        }
        val headerPath = channelPath.resolve("header")
        if (channel.header == EditingAction.Remove) {
            headerPath.deleteRecursively()
        } else if (channel.header is EditingAction.Edit) {
            if (headerPath.notExists()) {
                headerPath.createDirectory()
            } else {
                headerPath.listDirectoryEntries().forEach { it.deleteIfExists() }
            }
            val ext = channel.header.value.fileName.parseExtension()
            runBlocking {
                val smallImage = launch {
                    uploadImage(
                        uploadedFile = channel.header.value,
                        targetFile = "$headerPath/small.$ext",
                        width = 600,
                        height = 100,
                        compress = true
                    )
                }
                val mediumImage = launch {
                    uploadImage(
                        uploadedFile = channel.header.value,
                        targetFile = "$headerPath/medium.$ext",
                        width = 1200,
                        height = 200,
                        compress = true
                    )
                }
                val largeImage = launch {
                    uploadImage(
                        uploadedFile = channel.header.value,
                        targetFile = "$headerPath/large.$ext",
                        width = 2400,
                        height = 400,
                        compress = true
                    )
                }
                setOf(smallImage, mediumImage, largeImage).joinAll()
            }

        }
        val logoPath = channelPath.resolve("logo")
        if (channel.logo == EditingAction.Remove) {
            logoPath.deleteRecursively()
        } else if (channel.logo is EditingAction.Edit) {
            if (logoPath.notExists()) {
                logoPath.createDirectory()
            } else {
                logoPath.listDirectoryEntries().forEach { it.deleteIfExists() }
            }
            val ext = channel.logo.value.fileName.parseExtension()
            runBlocking {
                val smallImage = launch {
                    uploadImage(
                        uploadedFile = channel.logo.value,
                        targetFile = "$logoPath/small.$ext",
                        width = 64,
                        height = 64,
                        compress = true
                    )
                }
                val mediumImage = launch {
                    uploadImage(
                        uploadedFile = channel.logo.value,
                        targetFile = "$logoPath/medium.$ext",
                        width = 192,
                        height = 192,
                        compress = true
                    )
                }
                val largeImage = launch {
                    uploadImage(
                        uploadedFile = channel.logo.value,
                        targetFile = "$logoPath/large.$ext",
                        width = 512,
                        height = 512,
                        compress = true
                    )
                }
                setOf(smallImage, mediumImage, largeImage).joinAll()
            }
        }
        return editedChannel
    }

    @OptIn(ExperimentalPathApi::class)
    override fun removeChannel(userId: Long, channelId: Long) {
        if (!channelRepository.existsById(channelId)) {
            throw NoSuchElementException()
        } else if (!channelRepository.existsByOwnerIdAndChannelId(userId, channelId)) {
            throw IllegalAccessException()
        } else {
            val videoIds = videoRepository.findByChannelId(channelId).map { it.videoId!! }
            videoSearchRepository.deleteAllById(videoIds)
            runBlocking {
                videoIds.map {
                    launch(Dispatchers.IO) {
                        Path(appPaths.VIDEOS_BASE_PATH, it.toString()).deleteRecursively()
                    }
                }.joinAll()
            }
            channelRepository.deleteById(channelId)
            val channelPath = Path(appPaths.CHANNELS_BASE_PATH, channelId.toString())
            channelPath.deleteRecursively()
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

    private companion object {
        const val CHANNELS_TOPICS_PREFIX = "channels"
    }
}
