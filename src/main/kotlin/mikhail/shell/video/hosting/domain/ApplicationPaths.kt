package mikhail.shell.video.hosting.domain

object ApplicationPaths {
    private const val STORAGE_BASE_PATH = "C:/VideoHostingStorage"
    const val CHANNELS_BASE_PATH = "$STORAGE_BASE_PATH/channels"
    const val CHANNEL_COVERS_BASE_PATH = "$CHANNELS_BASE_PATH/covers"
    const val CHANNEL_AVATARS_BASE_PATH = "$CHANNELS_BASE_PATH/avatars"
    const val VIDEOS_BASE_PATH = "$STORAGE_BASE_PATH/videos"
    const val VIDEOS_PLAYABLES_BASE_PATH = "$VIDEOS_BASE_PATH/playables"
    const val VIDEOS_COVERS_BASE_PATH = "$VIDEOS_BASE_PATH/covers"
}