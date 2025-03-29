package mikhail.shell.video.hosting.errors

enum class GetUserError: Error {
    NOT_FOUND,
    UNEXPECTED
}

enum class EditUserError: Error {
    USER_NOT_FOUND,
    FORBIDDEN,
    NAME_TOO_LARGE,
    NICK_EMPTY,
    NICK_TOO_LARGE,
    AGE_MALFORMED,
    BIO_TOO_LARGE,
    TEL_MALFORMED,
    EMAIL_MALFORMED,
    EMAIL_TOO_LARGE,
    AVATAR_TOO_LARGE,
    AVATAR_MIME_NOT_SUPPORTED,
    UNEXPECTED
}

enum class RemoveUserError: Error {
    NOT_FOUND,
    FORBIDDEN,
    UNEXPECTED
}
