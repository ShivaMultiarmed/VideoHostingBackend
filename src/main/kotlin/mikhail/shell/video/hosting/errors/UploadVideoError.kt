package mikhail.shell.video.hosting.errors

enum class UploadVideoError: Error {
    TITLE_EMPTY,
    TITLE_TOO_LARGE,

    CHANNEL_NOT_VALID,

    SOURCE_EMPTY,
    SOURCE_TOO_LARGE,
    SOURCE_TYPE_NOT_VALID,

    COVER_TOO_LARGE,
    COVER_TYPE_NOT_VALID
}