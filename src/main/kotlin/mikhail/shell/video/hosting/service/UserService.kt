package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.EditAction
import mikhail.shell.video.hosting.domain.ImageSize
import mikhail.shell.video.hosting.domain.UploadedFile
import mikhail.shell.video.hosting.domain.User
import org.springframework.core.io.Resource

interface UserService {
    fun get(userId: Long): User
    fun edit(user: User, avatarAction: EditAction, avatar: UploadedFile?): User
    fun remove(userId: Long)
    fun existsByNick(userId: Long? = null, nick: String): Boolean
    fun getAvatar(userId: Long, size: ImageSize): Resource
}