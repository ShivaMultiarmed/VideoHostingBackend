package mikhail.shell.video.hosting.errors

enum class EditChannelError: Error {
    TITLE_EMPTY,
    TITLE_TOO_LARGE,
    TITLE_EXISTS,

    ALIAS_TOO_LARGE,
    ALIAS_EXISTS,

    DESCRIPTION_TOO_LARGE,

    COVER_TOO_LARGE,
    COVER_TYPE_NOT_VALID,

    AVATAR_TOO_LARGE,
    AVATAR_TYPE_NOT_VALID
}