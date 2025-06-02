package mikhail.shell.video.hosting.errors

enum class SignUpError: Error {
    USERNAME_EMPTY,
    USERNAME_MALFORMED,
    USERNAME_TOO_LARGE,
    USERNAME_EXISTS,

    PASSWORD_EMPTY,
    PASSWORD_NOT_VALID,

    NICK_EMPTY,
    NICK_TOO_LARGE,

    UNEXPECTED
}