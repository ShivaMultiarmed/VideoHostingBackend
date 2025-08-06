package mikhail.shell.video.hosting.service


import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.errors.CompoundError
import mikhail.shell.video.hosting.errors.EditUserError
import mikhail.shell.video.hosting.errors.ValidationException
import mikhail.shell.video.hosting.repository.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UserServiceWithDB @Autowired constructor(
    private val userRepository: UserRepository,
    private val channelService: ChannelService,
    private val authRepository: AuthRepository,
    private val commentService: CommentService
) : UserService {
    override fun get(userId: Long): User {
        val userEntity = userRepository.findById(userId).orElseThrow()
        return userEntity.toDomain()
    }

    override fun edit(user: User, avatarAction: EditAction, avatar: File?): User {
        val compoundError = CompoundError<EditUserError>()
        if (!userRepository.existsById(user.userId!!)) {
            throw NoSuchElementException()
        }
        if (user.nick.length > ValidationRules.MAX_NAME_LENGTH) {
            compoundError.add(EditUserError.NICK_TOO_LARGE)
        }
        if ((user.name?.length?: 0) > ValidationRules.MAX_NAME_LENGTH) {
            compoundError.add(EditUserError.NAME_TOO_LARGE)
        }
        if ((user.email?.length?: 0) > ValidationRules.MAX_USERNAME_LENGTH) {
            compoundError.add(EditUserError.EMAIL_TOO_LARGE)
        }
        if ((user.bio?.length ?: 0) > ValidationRules.MAX_TEXT_LENGTH) {
            compoundError.add(EditUserError.BIO_TOO_LARGE)
        }
        if ((avatar?.content?.size?: 0) > ValidationRules.MAX_IMAGE_SIZE) {
            compoundError.add(EditUserError.AVATAR_TOO_LARGE)
        }
        if (avatar?.mimeType?.substringBefore("/") != "image" && avatar != null) {
            compoundError.add(EditUserError.AVATAR_TYPE_NOT_VALID)
        }
        if (compoundError.isNotNull()) {
            throw ValidationException(compoundError)
        }
        val userEntity = user.toEntity()
        val editedUserEntity = userRepository.save(userEntity)
        if (avatarAction != EditAction.KEEP) {
            findFileByName(java.io.File(ApplicationPaths.USER_AVATARS_BASE_PATH), user.userId.toString())?.delete()
        }
        if (avatarAction == EditAction.UPDATE) {
            avatar?.let {
                val fileName = user.userId.toString() + "." + it.name!!.parseExtension()
                val file = java.io.File(ApplicationPaths.USER_AVATARS_BASE_PATH, fileName)
                file.createNewFile()
                file.writeBytes(it.content!!)
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
            channelService.removeChannel(it.channelId!!)
        }
        commentService.removeAllByUserId(userId)
        val credentialIds = authRepository.findById_UserId(userId).map { it.id }
        findFileByName(java.io.File(ApplicationPaths.USER_AVATARS_BASE_PATH), userId.toString())?.delete()
        authRepository.deleteAllById(credentialIds)
        userRepository.deleteById(userId)
    }

    override fun getAvatar(userId: Long): java.io.File {
        val file = findFileByName(java.io.File(ApplicationPaths.USER_AVATARS_BASE_PATH), userId.toString())
        if (file?.exists() != true) {
            throw NoSuchElementException()
        } else {
            return file
        }
    }
}