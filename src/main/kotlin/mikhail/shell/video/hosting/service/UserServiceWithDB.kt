package mikhail.shell.video.hosting.service


import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.dto.ChannelWithUserDto
import mikhail.shell.video.hosting.errors.CompoundError
import mikhail.shell.video.hosting.errors.EditUserError
import mikhail.shell.video.hosting.errors.HostingDataException
import mikhail.shell.video.hosting.repository.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UserServiceWithDB @Autowired constructor(
    private val userRepository: UserRepository,
    private val channelService: ChannelService,
    private val authRepository: AuthRepository
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
        if (user.nick.length > 255) {
            compoundError.add(EditUserError.NICK_TOO_LARGE)
        }
        if ((user.name?.length?: 0) > 255) {
            compoundError.add(EditUserError.NAME_TOO_LARGE)
        }
        if ((user.bio?.length ?: 0) > 5000) {
            compoundError.add(EditUserError.BIO_TOO_LARGE)
        }
        if ((avatar?.content?.size?: 0) > MAX_FILE_SIZE) {
            compoundError.add(EditUserError.AVATAR_TOO_LARGE)
        }
        if (avatar?.mimeType?.substringBefore("/") != "image" && avatar != null) {
            compoundError.add(EditUserError.AVATAR_MIME_NOT_SUPPORTED)
        }
        if (compoundError.isNotNull()) {
            throw HostingDataException(compoundError)
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

    override fun remove(userId: Long) {
        if (!userRepository.existsById(userId)) {
            throw NoSuchElementException()
        }
        channelService.getChannelsByOwnerId(userId).forEach {
            channelService.removeChannel(it.channelId!!)
        }
        val credentialIds = authRepository.findById_UserId(userId).map { it.id }
        findFileByName(java.io.File(ApplicationPaths.USER_AVATARS_BASE_PATH), userId.toString())?.delete()
        authRepository.deleteAllById(credentialIds)
        userRepository.deleteById(userId)
    }

    override fun getAvatar(userId: Long): ByteArray {
        val file = findFileByName(java.io.File(ApplicationPaths.USER_AVATARS_BASE_PATH), userId.toString())
        if (file?.exists() != true) {
            throw NoSuchElementException()
        } else {
            return file.readBytes()
        }
    }

    private companion object {
        const val MAX_FILE_SIZE = 10 * 1024 * 1024
    }
}