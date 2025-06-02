package mikhail.shell.video.hosting.errors

enum class EditVideoError: Error {
    VIDEO_NOT_FOUND,
    TITLE_EMPTY,
    TITLE_TOO_LARGE,
    COVER_EMPTY,
    COVER_TYPE_INVALID,
    COVER_TOO_LARGE,
    UNEXPECTED
}