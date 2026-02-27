package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.AuthModel
import mikhail.shell.video.hosting.domain.UserCreatingModel
import mikhail.shell.video.hosting.domain.UserNameCheckPurpose

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
        user: UserCreatingModel,
        token: String
    ): AuthModel
    fun requestPasswordReset(userName: String): Long
    fun verifyPasswordReset(
        userId: Long,
        code: String
    ): String

    fun confirmPasswordReset(
        token: String,
        password: String
    ): AuthModel

    fun existsByUserName(
        purpose: UserNameCheckPurpose,
        userName: String
    )

    fun signOut(token: String)
}