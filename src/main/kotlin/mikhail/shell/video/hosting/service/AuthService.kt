package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.AuthModel
import mikhail.shell.video.hosting.domain.UserCreatingModel

interface AuthService {
    fun signInWithPassword(
        userName: String,
        password: String
    ): AuthModel
    fun requestSignUpWithPassword(userName: String)
    fun verifySignUpWithPassword(
        userName: String,
        code: String
    ): String
    fun confirmSignUpWithPassword(user: UserCreatingModel): AuthModel
    fun requestPasswordReset(userName: String): Long
    fun verifyPasswordReset(
        userId: Long,
        code: String
    ): String
    fun confirmPasswordReset(
        userId: Long,
        password: String
    ): AuthModel
    fun existsByUserName(userName: String): Boolean
    fun signOut(token: String)
}