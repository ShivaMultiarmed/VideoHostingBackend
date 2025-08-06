package mikhail.shell.video.hosting.errors

enum class EditVideoError: Error {
    TITLE_EMPTY,
    TITLE_TOO_LARGE,

    COVER_EMPTY,
    COVER_TYPE_NOT_VALID,
    COVER_TOO_LARGE
}