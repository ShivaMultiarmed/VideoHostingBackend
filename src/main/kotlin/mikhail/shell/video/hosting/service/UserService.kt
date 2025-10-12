package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.*
import org.springframework.core.io.Resource

interface UserService {
    fun get(userId: Long): User
    fun edit(user: UserEditingModel): User
    fun remove(userId: Long)
    fun existsByNick(userId: Long? = null, nick: String): Boolean
    fun getAvatar(userId: Long, size: ImageSize): Resource
}