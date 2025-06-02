package mikhail.shell.video.hosting.domain

object ValidationRules {
    const val MAX_TITLE_LENGTH = 100
    const val MAX_NAME_LENGTH = 50
    const val MAX_TEXT_LENGTH = 255
    const val MAX_USERNAME_LENGTH = 50
    const val MIN_PASSWORD_LENGTH = 8
    const val MAX_PASSWORD_LENGTH = 20
    const val MAX_IMAGE_SIZE = 10 * 1024 * 1024
    const val MAX_VIDEO_SIZE = 100 * 1024 * 1024
}