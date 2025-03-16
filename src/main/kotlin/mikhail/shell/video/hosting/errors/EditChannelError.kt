package mikhail.shell.video.hosting.errors

enum class EditChannelError: Error {
    CHANNEL_NOT_EXIST,
    TITLE_EMPTY,
    TITLE_TOO_LARGE,
    COVER_TOO_LARGE,
    AVATAR_TOO_LARGE,
    UNEXPECTED
}