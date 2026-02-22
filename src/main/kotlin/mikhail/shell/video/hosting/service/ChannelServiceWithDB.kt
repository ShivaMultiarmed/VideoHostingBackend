package mikhail.shell.video.hosting.service

import com.google.firebase.messaging.FirebaseMessaging
import jakarta.transaction.Transactional
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
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
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.*

@Service
class ChannelServiceWithDB @Autowired constructor(
    private val channelRepository: ChannelRepository,
    private val userRepository: UserRepository,
    private val subscriberRepository: SubscriberRepository,
    private val videoRepository: VideoRepository,
    private val videoSearchRepository: VideoSearchRepository,
    private val fcm: FirebaseMessaging,
    private val appPaths: ApplicationPaths,
    private val imageValidator: FileValidator.ImageValidator
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

    @OptIn(ExperimentalPathApi::class)
    @Transactional
    override fun createChannel(channel: ChannelCreationModel): Channel {
        val errors = mutableMapOf<String, Error>()
        if (channelRepository.existsByTitle(channel.title)) {
            errors["title"] = TextError.EXISTS
        }
        if (channel.alias != null && channelRepository.existsByAlias(channel.alias)) {
            errors["alias"] = TextError.EXISTS
        }
        val tmpId = UUID.randomUUID()
        val tmpPath = Path(appPaths.TEMP_PATH, tmpId.toString()).createDirectory()
        runBlocking {
            val headerJob = channel.header?.let {
                val ext = it.name.parseExtension()
                val tmpHeaderPath = tmpPath.resolve("header.$ext")
                async(Dispatchers.IO) {
                    it.content.inputStream().uploadFile(
                        targetFile = tmpHeaderPath
                    )
                    imageValidator.validate(tmpHeaderPath.toFile())
                }
            }
            val logoJob = channel.logo?.let {
                val ext = it.name.parseExtension()
                val tmpLogoPath = tmpPath.resolve("logo.$ext")
                async(Dispatchers.IO) {
                    it.content.inputStream().uploadFile(
                        targetFile = tmpLogoPath
                    )
                    imageValidator.validate(tmpLogoPath.toFile())
                }
            }
            headerJob?.await()?.onFailure { error ->
                errors["header"] = error
            }
            logoJob?.await()?.onFailure { error ->
                errors["logo"] = error
            }
        }
        if (errors.isNotEmpty()) {
            tmpPath.deleteRecursively()
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
        val channelPath = Path(appPaths.CHANNELS_BASE_PATH, createdChannel.channelId.toString()).createDirectory()
        findFileByName(tmpPath, "header")?.let {
            val header = it.inputStream().toImage()?: return@let
            val tmpHeaderPath = tmpPath.resolve("header.${it.extension}")
            val headerDirectoryPath = channelPath.resolve("header").createDirectory()
            header.moveHeaders(
                tmpHeaderPath = tmpHeaderPath,
                headerDirectoryPath = headerDirectoryPath
            )
        }
        findFileByName(tmpPath, "logo")?.let {
            val logo = it.inputStream().toImage()?: return@let
            val tmpLogoPath = tmpPath.resolve("logo.${it.extension}")
            val logoDirectoryPath = channelPath.resolve("logo").createDirectory()
            logo.moveLogos(
                tmpLogoPath = tmpLogoPath,
                logoDirectoryPath = logoDirectoryPath
            )
        }
        tmpPath.deleteRecursively()
        return createdChannel
    }

    private fun BufferedImage.moveLogos(
        tmpLogoPath: Path,
        logoDirectoryPath: Path
    ): Job {
        val ext = tmpLogoPath.extension
        return runBlocking(Dispatchers.IO) {
            launch {
                uploadImage(
                    targetFile = logoDirectoryPath.resolve("small.$ext"),
                    targetWidth = 64,
                    targetHeight = 64,
                    compress = true
                )
            }
            launch {
                uploadImage(
                    targetFile = logoDirectoryPath.resolve("medium.$ext"),
                    targetWidth = 192,
                    targetHeight = 192,
                    compress = true
                )
            }
            launch {
                uploadImage(
                    targetFile = logoDirectoryPath.resolve("large.$ext"),
                    targetWidth = 512,
                    targetHeight = 512,
                    compress = true
                )
            }
        }
    }

    private fun BufferedImage.moveHeaders(
        tmpHeaderPath: Path,
        headerDirectoryPath: Path
    ): Job {
        val ext = tmpHeaderPath.extension
        return runBlocking(Dispatchers.IO) {
            launch {
                uploadImage(
                    targetFile = headerDirectoryPath.resolve("small.$ext"),
                    targetWidth = 600,
                    targetHeight = 100,
                    compress = true
                )
            }
            launch {
                uploadImage(
                    targetFile = headerDirectoryPath.resolve("medium.$ext"),
                    targetWidth = 1200,
                    targetHeight = 200,
                    compress = true
                )
            }
            launch {
                uploadImage(
                    targetFile = headerDirectoryPath.resolve("large.$ext"),
                    targetWidth = 2400,
                    targetHeight = 400,
                    compress = true
                )
            }
        }
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
        val topic = "channels.$channelId.subscribers"
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
            errors["title"] = TextError.EXISTS
        }
        if (channel.alias != null && channelRepository.existsByAlias(channel.alias) && currentChannelEntity.alias != channel.alias) {
            errors["alias"] = TextError.EXISTS
        }
        val tmpId = UUID.randomUUID()
        val tmpPath = Path(appPaths.TEMP_PATH, tmpId.toString()).createDirectory()
        runBlocking {
            supervisorScope {
                val headerJob = if (channel.header is EditingAction.Edit) {
                    val ext = channel.header.value.name.parseExtension()
                    val tmpHeaderPath = tmpPath.resolve("header.$ext")
                    async(Dispatchers.IO) {
                        channel.header.value.content.inputStream().uploadFile(
                            targetFile = tmpHeaderPath
                        )
                        imageValidator.validate(tmpHeaderPath.toFile())
                    }
                } else null
                val logoJob = if (channel.logo is EditingAction.Edit) {
                    val ext = channel.logo.value.name.parseExtension()
                    val tmpLogoPath = tmpPath.resolve("logo.$ext")
                    async(Dispatchers.IO) {
                        channel.logo.value.content.inputStream().uploadFile(
                            targetFile = tmpLogoPath
                        )
                        imageValidator.validate(tmpLogoPath.toFile())
                    }
                } else null
                headerJob?.await()?.onFailure { error ->
                    errors["header"] = error
                }
                logoJob?.await()?.onFailure { error ->
                    errors["logo"] = error
                }
            }
        }
        if (errors.isNotEmpty()) {
            tmpPath.deleteRecursively()
            throw ValidationException(errors)
        }
        val editedChannel = channelRepository.save(
            currentChannelEntity.copy(
                title = channel.title,
                alias = channel.alias,
                description = channel.description
            )
        ).toDomain()
        val channelPath = Path(appPaths.CHANNELS_BASE_PATH, editedChannel.channelId.toString()).createDirectories()
        val headerPath = channelPath.resolve("header")
        if (channel.header == EditingAction.Remove) {
            headerPath.deleteRecursively()
        } else if (channel.header is EditingAction.Edit) {
            if (headerPath.notExists()) {
                headerPath.createDirectory()
            } else {
                headerPath.listDirectoryEntries().forEach { it.deleteIfExists() }
            }
            findFileByName(tmpPath, "header")?.let {
                val header = it.inputStream().toImage()?: return@let
                val tmpHeaderPath = tmpPath.resolve("header.${it.extension}")
                header.moveHeaders(
                    tmpHeaderPath = tmpHeaderPath,
                    headerDirectoryPath = headerPath
                )
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
            findFileByName(tmpPath, "logo")?.let {
                val logo = it.inputStream().toImage()?: return@let
                val tmpLogoPath = tmpPath.resolve("logo.${it.extension}")
                val logoDirectoryPath = channelPath.resolve("logo").createDirectories()
                logo.moveLogos(
                    tmpLogoPath = tmpLogoPath,
                    logoDirectoryPath = logoDirectoryPath
                )
            }
        }
        tmpPath.deleteRecursively()
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
