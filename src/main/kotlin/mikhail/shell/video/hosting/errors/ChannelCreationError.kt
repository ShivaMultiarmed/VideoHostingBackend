package mikhail.shell.video.hosting.errors

enum class ChannelCreationError: Error {
    EXISTS,
    OWNER_NOT_CHOSEN,
    TITLE_EMPTY,
    TITLE_TOO_LARGE,
    DESCRIPTION_TOO_LARGE,
    COVER_TYPE_NOT_VALID,
    COVER_TOO_LARGE,
    AVATAR_TYPE_NOT_VALID,
    AVATAR_TOO_LARGE,
    UNEXPECTED
}