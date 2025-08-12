package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.EditAction
import mikhail.shell.video.hosting.domain.UploadedFile
import mikhail.shell.video.hosting.domain.User

interface UserService {
    fun get(userId: Long): User
    fun edit(user: User, avatarAction: EditAction, avatar: UploadedFile?): User
    fun remove(userId: Long)
    fun getAvatar(userId: Long): java.io.File
    fun checkExistence(userId: Long): Boolean
}