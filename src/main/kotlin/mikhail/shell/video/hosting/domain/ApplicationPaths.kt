package mikhail.shell.video.hosting.domain

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class ApplicationPathsInitializer(
    @Value("\${video-hosting.storage.path}") val STORAGE_BASE_PATH: String,
) {
    val CHANNELS_BASE_PATH = "$STORAGE_BASE_PATH/channels"
    val VIDEOS_BASE_PATH = "$STORAGE_BASE_PATH/videos"
    val USERS_BASE_PATH = "$STORAGE_BASE_PATH/users"
    val TEMP_PATH = "$STORAGE_BASE_PATH/tmp"
}