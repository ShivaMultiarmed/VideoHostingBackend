package mikhail.shell.video.hosting.errors

enum class EditChannelError: Error {
    CHANNEL_NOT_EXIST,

    TITLE_EMPTY,
    TITLE_TOO_LARGE,

    ALIAS_TOO_LARGE,

    DESCRIPTION_TOO_LARGE,

    COVER_EMPTY,
    COVER_TOO_LARGE,
    COVER_TYPE_INVALID,

    AVATAR_EMPTY,
    AVATAR_TOO_LARGE,
    AVATAR_TYPE_INVALID,

    UNEXPECTED
}