package mikhail.shell.video.hosting.domain

object ValidationRules {
    const val CODE_LENGTH = 4
    const val MAX_TITLE_LENGTH = 100
    const val MAX_NAME_LENGTH = 50
    const val MAX_TEXT_LENGTH = 255
    const val MAX_USERNAME_LENGTH = 50
    const val MAX_IMAGE_SIZE = 10 * 1024 * 1024
    const val MAX_VIDEO_SIZE = 512 * 1024 * 1024
    val PASSWORD_REGEX = Regex("^(?=.*[0-9])(?=.*[^a-zA-Z0-9])\\S{8,20}$")
    val EMAIL_REGEX = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$")
}