package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.EditAction
import mikhail.shell.video.hosting.domain.File
import mikhail.shell.video.hosting.domain.User
import mikhail.shell.video.hosting.errors.CompoundError
import mikhail.shell.video.hosting.errors.EditUserError
import mikhail.shell.video.hosting.errors.HostingDataException
import mikhail.shell.video.hosting.repository.UserRepository
import mikhail.shell.video.hosting.repository.toDomain
import mikhail.shell.video.hosting.repository.toEntity
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UserServiceWithDB @Autowired constructor(
    private val userRepository: UserRepository
) : UserService {
    override fun get(userId: Long): User {
        val userEntity = userRepository.findById(userId).orElseThrow()
        return userEntity.toDomain()
    }

    override fun edit(user: User, avatarAction: EditAction, avatar: File): User {
        val compoundError = CompoundError<EditUserError>()
        if (userRepository.existsById(user.userId!!)) {
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
        if (compoundError.isNotNull()) {
            throw HostingDataException(compoundError)
        }
        val userEntity = user.toEntity()
        val editedUserEntity = userRepository.save(userEntity)
        return editedUserEntity.toDomain()
    }

    override fun remove(userId: Long) {
        if (!userRepository.existsById(userId)) {
            throw NoSuchElementException()
        }
        userRepository.deleteById(userId)
    }
}