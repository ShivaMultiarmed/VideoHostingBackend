package mikhail.shell.video.hosting.errors

enum class UploadVideoError: Error {
    UNEXPECTED, TITLE_EMPTY, TITLE_TOO_LARGE
}