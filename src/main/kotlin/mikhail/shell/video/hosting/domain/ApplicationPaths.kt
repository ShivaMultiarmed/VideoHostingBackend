package mikhail.shell.video.hosting.domain

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class ApplicationPathsInitializer(
    @Value("\${hosting.storage.path}") val STORAGE_BASE_PATH: String,
) {
    val CHANNELS_BASE_PATH = "$STORAGE_BASE_PATH/channels"
    val CHANNEL_HEADERS_BASE_PATH = "$CHANNELS_BASE_PATH/headers"
    val CHANNEL_LOGOS_BASE_PATH = "$CHANNELS_BASE_PATH/logos"
    val VIDEOS_BASE_PATH = "$STORAGE_BASE_PATH/videos"
    val VIDEOS_SOURCES_BASE_PATH = "$VIDEOS_BASE_PATH/sources"
    val VIDEOS_COVERS_BASE_PATH = "$VIDEOS_BASE_PATH/covers"
    val USERS_BASE_PATH = "$STORAGE_BASE_PATH/users"
    val USER_AVATARS_BASE_PATH = "$USERS_BASE_PATH/avatars"
    val TEMP_PATH = "$STORAGE_BASE_PATH/tmp"
    val TEMP_VIDEOS_BASE_PATH = "$TEMP_PATH/videos"
}