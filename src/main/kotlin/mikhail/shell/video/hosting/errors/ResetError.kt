package mikhail.shell.video.hosting.errors

enum class ResetError: Error {
    USERNAME_NOT_FOUND,
    USERNAME_EMPTY,
    USERNAME_MALFORMED,

    CODE_NOT_CORRECT,
    CODE_NOT_VALID,

    TOKEN_NOT_VALID,

    PASSWORD_EMPTY,
    PASSWORD_NOT_VALID
}