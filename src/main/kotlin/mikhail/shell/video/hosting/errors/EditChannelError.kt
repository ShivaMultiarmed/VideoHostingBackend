package mikhail.shell.video.hosting.errors

enum class EditChannelError: Error {
    CHANNEL_NOT_EXIST,

    TITLE_EMPTY,
    TITLE_TOO_LARGE,
    TITLE_EXISTS,

    ALIAS_TOO_LARGE,
    ALIAS_EXISTS,

    DESCRIPTION_TOO_LARGE,

    COVER_EMPTY,
    COVER_TOO_LARGE,
    COVER_TYPE_NOT_VALID,

    AVATAR_EMPTY,
    AVATAR_TOO_LARGE,
    AVATAR_TYPE_NOT_VALID,

    UNEXPECTED
}