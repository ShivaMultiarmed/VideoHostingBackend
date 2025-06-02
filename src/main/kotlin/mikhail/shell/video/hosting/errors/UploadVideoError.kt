package mikhail.shell.video.hosting.errors

enum class UploadVideoError: Error {
    TITLE_EMPTY,
    TITLE_TOO_LARGE,
    CHANNEL_INVALID,
    SOURCE_EMPTY,
    SOURCE_TOO_LARGE,
    SOURCE_TYPE_INVALID,
    COVER_EMPTY,
    COVER_TYPE_INVALID,
    COVER_TOO_LARGE,
    UNEXPECTED
}