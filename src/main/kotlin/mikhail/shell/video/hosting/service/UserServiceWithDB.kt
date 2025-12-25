package mikhail.shell.video.hosting.service


import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.errors.Error
import mikhail.shell.video.hosting.errors.TextError
import mikhail.shell.video.hosting.errors.ValidationException
import mikhail.shell.video.hosting.repository.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import javax.annotation.PreDestroy
import kotlin.io.path.*

@Service
class UserServiceWithDB @Autowired constructor(
    private val userRepository: UserRepository,
    private val channelService: ChannelService,
    private val subscriberRepository: SubscriberRepository,
    private val authDetailRepository: AuthDetailRepository,
    private val commentService: CommentService,
    private val fcm: FirebaseMessaging,
    private val appPaths: ApplicationPathsInitializer
) : UserService {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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
        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }
        val userToEdit = userRepository.findById(user.userId).get()
        val editedUserEntity = userRepository.save(
            userToEdit.copy(
                nick = user.nick,
                name = user.name,
                bio = user.bio,
                tel = user.tel,
                email = user.email
            )
        )
        val userPath = Path(appPaths.USERS_BASE_PATH, user.userId.toString())
        if (userPath.notExists()) {
            userPath.createDirectory()
        }
        val avatarPath = userPath.resolve("avatar")
        if (user.avatar is EditingAction.Remove) {
            avatarPath.deleteRecursively()
        } else if (user.avatar is EditingAction.Edit) {
            if (avatarPath.notExists()) {
                avatarPath.createDirectory()
            } else {
                avatarPath.listDirectoryEntries().forEach { it.deleteIfExists() }
            }
            val ext = user.avatar.value.fileName.parseExtension()
            uploadImage(
                uploadedFile = user.avatar.value,
                targetFile = "$avatarPath/small.$ext",
                width = 64,
                height = 64,
                compress = true
            )
            uploadImage(
                uploadedFile = user.avatar.value,
                targetFile = "$avatarPath/medium.$ext",
                width = 192,
                height = 192,
                compress = true
            )
            uploadImage(
                uploadedFile = user.avatar.value,
                targetFile = "$avatarPath/large.$ext",
                width = 512,
                height = 512,
                compress = true
            )

        }
        return editedUserEntity.toDomain()
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
        channelService.getChannelsByOwnerId(userId).forEach {
            channelService.removeChannel(userId, it.channelId)
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
        subscriberRepository
            .findById_UserId(userId)
            .map { it.id.channelId }
            .forEach {
                coroutineScope.launch {
                    fcm.subscribeToTopic(listOf(token), "channels.$it.subscribers")
                }
            }
        channelService.getChannelsByOwnerId(userId)
            .map { it.channelId }
            .forEach {
                coroutineScope.launch {
                    fcm.subscribeToTopic(listOf(token), "channels.$it.uploads")
                }
            }
    }

    override fun unsubscribeFromNotifications(userId: Long, token: String) {
        subscriberRepository
            .findById_UserId(userId)
            .map { it.id.channelId }
            .forEach {
                coroutineScope.launch {
                    fcm.unsubscribeFromTopic(listOf(token), "channels.$it.subscribers")
                }
            }
        channelService.getChannelsByOwnerId(userId)
            .map { it.channelId }
            .forEach {
                coroutineScope.launch {
                    fcm.unsubscribeFromTopic(listOf(token), "channels.$it.uploads")
                }
            }
    }
    @PreDestroy
    fun preDestroy() {
        coroutineScope.cancel()
    }
}