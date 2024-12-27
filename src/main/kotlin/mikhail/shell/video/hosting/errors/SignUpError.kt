package mikhail.shell.video.hosting.errors

enum class SignUpError: Error {
    EMAIL_EMPTY, PASSWORD_EMPTY, NAME_EMPTY, EMAIL_EXISTS, PASSWORD_INVALID, UNEXPECTED
}