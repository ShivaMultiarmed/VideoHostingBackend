package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.*
import org.springframework.core.io.Resource
import java.io.File

interface VideoService {
    fun get(videoId: Long): Video
    fun get(videoId: Long, userId: Long): VideoWithUser
    fun checkLiking(videoId: Long, userId: Long): Liking
    fun rate(videoId: Long, userId: Long, liking: Liking): VideoWithUser
    fun getByChannelId(channelId: Long, partSize: Int, partNumber: Long): List<Video>
    fun getByQuery(query: String, partSize: Int, partNumber: Long): List<VideoWithChannel>
    fun incrementViews(videoId: Long): Video
    fun delete(userId: Long, videoId: Long)
    fun edit(userId: Long, video: Video, coverAction: EditAction, cover: UploadedFile?): Video
    fun saveVideoSource(videoId: Long, source: UploadedFile): Boolean
    fun confirm(userId: Long, videoId: Long)
    fun checkOwner(userId: Long, videoId: Long): Boolean
    fun getRecommendations(userId: Long, partIndex: Long, partSize: Int): List<VideoWithChannel>
    fun checkExistence(videoId: Long): Boolean
    fun sync()
    fun save(userId: Long, video: Video, cover: UploadedFile?): Video
    fun getCover(videoId: Long): Resource
}