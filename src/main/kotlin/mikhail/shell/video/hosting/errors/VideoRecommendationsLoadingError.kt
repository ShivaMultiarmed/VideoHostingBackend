package mikhail.shell.video.hosting.errors

enum class VideoRecommendationsLoadingError: Error {
    USER_ID_NOT_FOUND,
    PART_INDEX_NOT_VALID,
    PART_SIZE_NOT_VALID,
    UNEXPECTED
}