package mikhail.shell.video.hosting.domain

import mikhail.shell.video.hosting.domain.ApplicationPaths.CHANNELS_BASE_PATH
import mikhail.shell.video.hosting.domain.ApplicationPaths.STORAGE_BASE_PATH
import mikhail.shell.video.hosting.domain.ApplicationPaths.USERS_BASE_PATH
import mikhail.shell.video.hosting.domain.ApplicationPaths.VIDEOS_BASE_PATH
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class ApplicationPathsInitializer(
    @Value("\${hosting.storage.path}") storageBasePath: String
) {
    init {
        ApplicationPaths.STORAGE_BASE_PATH = storageBasePath
        ApplicationPaths.CHANNELS_BASE_PATH = "$STORAGE_BASE_PATH/channels"
        ApplicationPaths.CHANNEL_HEADERS_BASE_PATH = "$CHANNELS_BASE_PATH/covers"
        ApplicationPaths.CHANNEL_LOGOS_BASE_PATH = "$CHANNELS_BASE_PATH/avatars"
        ApplicationPaths.VIDEOS_BASE_PATH = "$STORAGE_BASE_PATH/videos"
        ApplicationPaths.VIDEOS_PLAYABLES_BASE_PATH = "$VIDEOS_BASE_PATH/playables"
        ApplicationPaths.VIDEOS_COVERS_BASE_PATH = "$VIDEOS_BASE_PATH/covers"
        ApplicationPaths.USERS_BASE_PATH = "$STORAGE_BASE_PATH/users"
        ApplicationPaths.USER_AVATARS_BASE_PATH = "$USERS_BASE_PATH/avatars"
    }
}

object ApplicationPaths {
    lateinit var STORAGE_BASE_PATH : String
    lateinit var CHANNELS_BASE_PATH: String
    lateinit var CHANNEL_HEADERS_BASE_PATH: String
    lateinit var CHANNEL_LOGOS_BASE_PATH: String
    lateinit var VIDEOS_BASE_PATH: String
    lateinit var VIDEOS_PLAYABLES_BASE_PATH: String
    lateinit var VIDEOS_COVERS_BASE_PATH: String
    lateinit var USERS_BASE_PATH: String
    lateinit var USER_AVATARS_BASE_PATH: String
}