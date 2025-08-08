package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.AuthModel
import mikhail.shell.video.hosting.domain.User

interface AuthenticationService {
    fun signInWithPassword(
        userName: String,
        password: String
    ): AuthModel
    fun signUpWithPassword(
        userName: String,
        password: String,
        user: User?
    ): AuthModel
    fun requestPasswordReset(
        userName: String
    )
    fun resetPassword(
        resetToken: String,
        password: String
    )
    fun verifyPasswordReset(
        userId: Long,
        resetCode: String
    ): String
}