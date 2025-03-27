package mikhail.shell.video.hosting.errors

enum class CommentError: Error {
    TEXT_EMPTY,
    TEXT_TOO_LARGE,
    NOT_FOUND,
    UNEXPECTED
}