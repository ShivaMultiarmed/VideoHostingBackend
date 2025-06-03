package mikhail.shell.video.hosting.errors

enum class ChannelCreationError: Error {
    OWNER_NOT_CHOSEN,

    TITLE_EMPTY,
    TITLE_TOO_LARGE,
    TITLE_EXISTS,

    ALIAS_TOO_LARGE,
    ALIAS_EXISTS,

    DESCRIPTION_TOO_LARGE,

    COVER_EMPTY,
    COVER_TYPE_NOT_VALID,
    COVER_TOO_LARGE,

    AVATAR_EMPTY,
    AVATAR_TYPE_NOT_VALID,
    AVATAR_TOO_LARGE,

    UNEXPECTED
}