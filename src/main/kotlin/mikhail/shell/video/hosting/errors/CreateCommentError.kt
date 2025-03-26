package mikhail.shell.video.hosting.errors

enum class CreateCommentError: Error {
    TEXT_EMPTY,
    TEXT_TOO_LARGE,
    UNEXPECTED
}