package mikhail.shell.video.hosting.errors

enum class VideoLoadingError: Error {
    VIDEO_NOT_FOUND,
    CHANNEL_NOT_FOUND,
    USER_NOT_FOUND,
    UNEXPECTED
}