package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.AuthModel
import mikhail.shell.video.hosting.domain.User
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
    fun confirmSignUpWithPassword(
        token: String,
        password: String,
        user: UserCreatingModel,
    ): AuthModel
    fun requestPasswordReset(userName: String)
    fun resetPassword(
        token: String,
        password: String
    )
    fun signOut(token: String)
    fun verifyPasswordReset(userName: String, code: String): String
    fun existsByUserName(userName: String): Boolean
}