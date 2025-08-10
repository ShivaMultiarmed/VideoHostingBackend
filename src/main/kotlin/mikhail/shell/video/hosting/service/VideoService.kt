package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.*

interface VideoService {
    fun getVideoInfo(videoId: Long): Video
    fun getVideoForUser(videoId: Long, userId: Long): VideoWithUser
    fun checkVideoLikeState(videoId: Long, userId: Long): LikingState
    fun rate(videoId: Long, userId: Long, likingState: LikingState)
    fun getVideosByChannelId(channelId: Long, partSize: Int, partNumber: Long): List<Video>
    fun getVideosByQuery(query: String, partSize: Int, partNumber: Long): List<VideoWithChannel>
    fun incrementViews(videoId: Long)
    fun deleteVideo(videoId: Long): Boolean
    fun editVideo(video: Video, coverAction: EditAction, cover: File?): Video
    fun saveVideoDetails(video: Video): Video
    fun saveVideoSource(videoId: Long, source: File): Boolean
    fun saveVideoCover(videoId: Long, cover: File): Boolean
    fun confirmVideoUpload(videoId: Long): Boolean
    fun checkOwner(userId: Long, videoId: Long): Boolean
    fun getRecommendedVideos(userId: Long, partIndex: Long, partSize: Int): List<VideoWithChannel>
    fun checkExistence(videoId: Long): Boolean
}