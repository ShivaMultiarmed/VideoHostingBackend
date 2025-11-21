package mikhail.shell.video.hosting.service


import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.errors.Error
import mikhail.shell.video.hosting.errors.TextError
import mikhail.shell.video.hosting.errors.ValidationException
import mikhail.shell.video.hosting.repository.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import kotlin.io.path.*

@Service
class UserServiceWithDB @Autowired constructor(
    private val userRepository: UserRepository,
    private val channelService: ChannelService,
    private val authDetailRepository: AuthDetailRepository,
    private val commentService: CommentService,
    private val appPaths: ApplicationPathsInitializer
) : UserService {
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
        if (user.avatarAction == EditAction.REMOVE) {
            avatarPath.deleteRecursively()
        } else if (user.avatarAction == EditAction.UPDATE) {
            user.avatar?.let {
                if (avatarPath.notExists()) {
                    avatarPath.createDirectory()
                }
                uploadImage(
                    uploadedFile = it,
                    targetFile = "$avatarPath/large.png",
                    width = 512,
                    height = 512
                )
                uploadImage(
                    uploadedFile = it,
                    targetFile = "$avatarPath/medium.png",
                    width = 128,
                    height = 128
                )
                uploadImage(
                    uploadedFile = it,
                    targetFile = "$avatarPath/small.png",
                    width = 64,
                    height = 64
                )
            }
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

    override fun remove(userId: Long) {
        if (!userRepository.existsById(userId)) {
            throw NoSuchElementException()
        }
        channelService.getChannelsByOwnerId(userId).forEach {
            channelService.removeChannel(userId, it.channelId)
        }
        commentService.removeAllByUserId(userId)
        val credentialIds = authDetailRepository.findById_UserId(userId).map { it.id }
        findFileByName(appPaths.USER_AVATARS_BASE_PATH, userId.toString())?.delete()
        authDetailRepository.deleteAllById(credentialIds)
        userRepository.deleteById(userId)
    }

    override fun getAvatar(userId: Long, size: ImageSize): Resource {
        val file = Path(appPaths.USERS_BASE_PATH, userId.toString(), "avatar", size.name.lowercase() + ".png").toFile()
        if (!userRepository.existsById(userId) || !file.exists()) {
            throw NoSuchElementException()
        } else {
            return FileSystemResource(file)
        }
    }
}