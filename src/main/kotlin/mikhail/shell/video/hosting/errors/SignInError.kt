package mikhail.shell.video.hosting.errors

enum class SignInError: Error {
    USERNAME_EMPTY,
    USERNAME_MALFORMED,

    PASSWORD_EMPTY,
    PASSWORD_INCORRECT
}