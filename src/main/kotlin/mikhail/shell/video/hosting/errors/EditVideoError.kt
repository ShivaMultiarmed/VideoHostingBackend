package mikhail.shell.video.hosting.errors

enum class EditVideoError: Error {
    UNEXPECTED,
    TITLE_EMPTY,
    TITLE_TOO_LARGE,
    VIDEO_NOT_FOUND
}