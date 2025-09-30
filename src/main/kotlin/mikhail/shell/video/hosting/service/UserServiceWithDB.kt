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

    override fun edit(user: User, avatarAction: EditAction, avatar: UploadedFile?): User {
        if (!userRepository.existsById(user.userId!!)) {
            throw NoSuchElementException()
        }
        val errors = mutableMapOf<String, Error>()
        if (userRepository.existsByNick(user.nick)) {
            errors["nickError"] = TextError.EXISTS
        }
        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }
        val editedUserEntity = userRepository.save(user.toEntity())
        if (avatarAction != EditAction.KEEP) {
            findFileByName(appPaths.USER_AVATARS_BASE_PATH, user.userId.toString())?.delete()
        }
        if (avatarAction == EditAction.UPDATE) {
            avatar?.let {
                uploadImage(
                    uploadedFile = it,
                    targetFile = "${appPaths.USER_AVATARS_BASE_PATH}/${user.userId}.jpg",
                    width = 480,
                    height = 480
                )
            }
        }
        return editedUserEntity.toDomain()
    }

    override fun checkExistence(userId: Long): Boolean {
        return userRepository.existsById(userId)
    }

    override fun remove(userId: Long) {
        if (!userRepository.existsById(userId)) {
            throw NoSuchElementException()
        }
        channelService.getChannelsByOwnerId(userId).forEach {
            channelService.removeChannel(userId, it.channelId!!)
        }
        commentService.removeAllByUserId(userId)
        val credentialIds = authDetailRepository.findById_UserId(userId).map { it.id }
        findFileByName(appPaths.USER_AVATARS_BASE_PATH, userId.toString())?.delete()
        authDetailRepository.deleteAllById(credentialIds)
        userRepository.deleteById(userId)
    }

    override fun getAvatar(userId: Long): Resource {
        return FileSystemResource(
            findFileByName(appPaths.USER_AVATARS_BASE_PATH, userId.toString())
                .takeUnless { !userRepository.existsById(userId) || it?.exists() != true }
                ?: throw NoSuchElementException()
        )
    }
}