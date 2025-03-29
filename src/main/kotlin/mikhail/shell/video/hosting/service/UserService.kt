package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.EditAction
import mikhail.shell.video.hosting.domain.File
import mikhail.shell.video.hosting.domain.User

interface UserService {
    fun get(userId: Long): User
    fun edit(user: User, avatarAction: EditAction, avatar: File?): User
    fun remove(userId: Long)
}