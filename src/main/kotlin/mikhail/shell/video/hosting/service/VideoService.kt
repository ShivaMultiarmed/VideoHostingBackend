package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.*
import org.springframework.core.io.Resource
import java.io.InputStream
import java.util.*

interface VideoService {
    fun get(videoId: Long): Video
    fun get(videoId: Long, userId: Long): VideoWithUser
    fun checkLiking(videoId: Long, userId: Long): Liking
    fun rate(videoId: Long, userId: Long, liking: Liking): VideoWithUser
    fun getByChannelId(channelId: Long, partSize: Int, partIndex: Long): List<Video>
    fun getByQuery(query: String, partSize: Int, cursor: Long?): List<VideoWithChannel>
    fun incrementViews(videoId: Long): Video
    fun remove(userId: Long, videoId: Long)
    fun edit(userId: Long, video: VideoEditingModel): Video
    fun getRecommendations(userId: Long, partIndex: Long, partSize: Int): List<VideoWithChannel>
    fun checkExistence(videoId: Long): Boolean
    fun sync()
    fun getCover(videoId: Long, size: ImageSize): Resource
    fun save(userId: Long, video: VideoCreationModel): PendingVideo
    fun saveVideoSource(userId: Long, tmpId: UUID, start: Long, end: Long, source: InputStream)
    fun confirm(userId: Long, tmpId: UUID)
}