package mikhail.shell.video.hosting.service


import com.google.firebase.messaging.FirebaseMessaging
import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.errors.Error
import mikhail.shell.video.hosting.errors.TextError
import mikhail.shell.video.hosting.errors.ValidationException
import mikhail.shell.video.hosting.repository.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.StructuredTaskScope
import javax.annotation.PreDestroy
import kotlin.io.path.*
import kotlin.use

@Service
class UserServiceWithDB @Autowired constructor(
    private val userRepository: UserRepository,
    private val channelService: ChannelService,
    private val subscriberRepository: SubscriberRepository,
    private val authDetailRepository: AuthDetailRepository,
    private val commentService: CommentService,
    private val fcm: FirebaseMessaging,
    private val appPaths: ApplicationPaths,
    private val imageValidator: FileValidator.ImageValidator
) : UserService {
    private val executorService = Executors.newVirtualThreadPerTaskExecutor()

    override fun get(userId: Long): User {
        return userRepository.findById(userId).orElseThrow().toDomain()
    }

    @OptIn(ExperimentalPathApi::class)
    override fun edit(user: UserEditingModel): User {
        if (!userRepository.existsById(user.userId)) {
            throw NoSuchElementException()
        }
        val errors = mutableMapOf<String, Error>()
        if (userRepository.existsByNick(user.nick) && !userRepository.existsByUserIdAndNick(user.userId, user.nick)) {
            errors["nick"] = TextError.EXISTS
        }
        val tmpId = UUID.randomUUID()
        val tmpPath = Path(appPaths.TEMP_PATH, tmpId.toString()).createDirectory()
        if (user.avatar is EditingAction.Edit) {
            val ext = user.avatar.value.name.parseExtension()
            val tmpAvatarPath = tmpPath.resolve("avatar.$ext")
            user.avatar.value.content.inputStream().uploadFile(tmpAvatarPath)
            imageValidator.validate(tmpAvatarPath.toFile()).onFailure { error ->
                errors["avatar"] = error
            }
        }
        if (errors.isNotEmpty()) {
            tmpPath.deleteRecursively()
            throw ValidationException(errors)
        }
        val userToEdit = userRepository.findById(user.userId).get()
        val editedUser = userRepository.save(
            userToEdit.copy(
                nick = user.nick,
                name = user.name,
                bio = user.bio,
                tel = user.tel,
                email = user.email
            )
        ).toDomain()
        val userPath = Path(appPaths.USERS_BASE_PATH, user.userId.toString()).createDirectories()
        val avatarPath = userPath.resolve("avatar")
        if (user.avatar is EditingAction.Remove) {
            avatarPath.deleteRecursively()
        } else if (user.avatar is EditingAction.Edit) {
            val ext = user.avatar.value.name.parseExtension()
            val tmpAvatarPath = tmpPath.resolve("avatar.$ext")
            val avatar = tmpAvatarPath.inputStream().toImage()
            if (avatarPath.notExists()) {
                avatarPath.createDirectory()
            } else {
                avatarPath.listDirectoryEntries().forEach { it.deleteIfExists() }
            }
            avatar?.moveAvatars(
                tmpAvatarPath = tmpAvatarPath,
                avatarDirectoryPath = avatarPath
            )
        }
        tmpPath.deleteRecursively()
        return editedUser
    }

    private fun BufferedImage.moveAvatars(
        tmpAvatarPath: Path,
        avatarDirectoryPath: Path
    ) {
        StructuredTaskScope.open(
            StructuredTaskScope.Joiner.awaitAll<Boolean>()
        ).use { scope ->
            val callables = setOf(
                Callable {
                    uploadImage(
                        targetFile = avatarDirectoryPath.resolve("small.${tmpAvatarPath.extension}"),
                        targetWidth = 64,
                        targetHeight = 64,
                        compress = true
                    )
                },
                Callable {
                    uploadImage(
                        targetFile = avatarDirectoryPath.resolve("medium.${tmpAvatarPath.extension}"),
                        targetWidth = 192,
                        targetHeight = 192,
                        compress = true
                    )
                },
                Callable {
                    uploadImage(
                        targetFile = avatarDirectoryPath.resolve("large.${tmpAvatarPath.extension}"),
                        targetWidth = 512,
                        targetHeight = 512,
                        compress = true
                    )
                }
            )
            callables.forEach(scope::fork)
            scope.join()
        }
    }

    override fun existsByNick(userId: Long?, nick: String): Boolean {
        return if (userId == null) {
            userRepository.existsByNick(nick)
        } else {
            userRepository.existsByUserIdAndNick(userId, nick)
        }
    }

    @OptIn(ExperimentalPathApi::class)
    override fun remove(userId: Long) {
        if (!userRepository.existsById(userId)) {
            throw NoSuchElementException()
        }
        StructuredTaskScope.open(
            StructuredTaskScope.Joiner.awaitAll<Void>()
        ).use { scope ->
            channelService.getChannelsByOwnerId(userId).forEach { channel ->
                val runnable = Runnable {
                    channelService.removeChannel(userId, channel.channelId)
                }
                scope.fork<Void>(runnable)
            }
            scope.join()
        }
        commentService.removeAllByUserId(userId)
        val credentialIds = authDetailRepository.findById_UserId(userId).map { it.id }
        Path(appPaths.USERS_BASE_PATH, userId.toString()).deleteRecursively()
        authDetailRepository.deleteAllById(credentialIds)
        userRepository.deleteById(userId)
    }

    override fun getAvatar(userId: Long, size: ImageSize): Resource {
        val avatarDirectory = Path(appPaths.USERS_BASE_PATH, userId.toString(), "avatar")
        val file = findFileByName(avatarDirectory, size.name.lowercase())
        if (!userRepository.existsById(userId) || file == null) {
            throw NoSuchElementException()
        } else {
            return FileSystemResource(file)
        }
    }

    override fun subscribeToNotifications(userId: Long, token: String) {
        val subscribedIds = subscriberRepository.findById_UserId(userId).map { it.id.channelId }
        val ownedIds = channelService.getChannelsByOwnerId(userId).map { it.channelId }
        executorService.execute {
            StructuredTaskScope.open(
                StructuredTaskScope.Joiner.awaitAll<Void>()
            ).use { scope ->
                StructuredTaskScope.open(
                    StructuredTaskScope.Joiner.awaitAll<Void>()
                ).use { scope ->
                    subscribedIds.forEach {
                        val runnable = Runnable {
                            fcm.subscribeToTopic(listOf(token), "channels.$it.subscribers")
                        }
                        scope.fork<Void>(runnable)
                    }
                    scope.join()
                }
                StructuredTaskScope.open(
                    StructuredTaskScope.Joiner.awaitAll<Void>()
                ).use { scope ->
                    ownedIds.forEach {
                        val runnable = Runnable {
                            fcm.subscribeToTopic(listOf(token), "channels.$it.uploads")
                        }
                        scope.fork<Void>(runnable)
                    }
                    scope.join()
                }
                scope.join()
            }
        }
    }

    override fun unsubscribeFromNotifications(userId: Long, token: String) {
        val subscribedIds = subscriberRepository.findById_UserId(userId).map { it.id.channelId }
        val ownedIds = channelService.getChannelsByOwnerId(userId).map { it.channelId }
        executorService.execute {
            StructuredTaskScope.open(
                StructuredTaskScope.Joiner.awaitAll<Void>()
            ).use { scope ->
                StructuredTaskScope.open(
                    StructuredTaskScope.Joiner.awaitAll<Void>()
                ).use { scope ->
                    subscribedIds.forEach {
                        val runnable = Runnable {
                            fcm.unsubscribeFromTopic(listOf(token), "channels.$it.subscribers")
                        }
                        scope.fork<Void>(runnable)
                    }
                    scope.join()
                }
                StructuredTaskScope.open(
                    StructuredTaskScope.Joiner.awaitAll<Void>()
                ).use { scope ->
                    ownedIds.forEach {
                        val runnable = Runnable {
                            fcm.unsubscribeFromTopic(listOf(token), "channels.$it.uploads")
                        }
                        scope.fork<Void>(runnable)
                    }
                    scope.join()
                }
                scope.join()
            }
        }
    }

    @PreDestroy
    fun preDestroy() {
        executorService.close()
    }
}