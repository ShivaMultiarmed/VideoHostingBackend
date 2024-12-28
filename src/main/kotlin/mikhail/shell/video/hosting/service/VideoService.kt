package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.*

interface VideoService {
    fun getVideoInfo(videoId: Long): Video
    fun getVideoForUser(videoId: Long, userId: Long): VideoWithUser
    fun checkVideoLikeState(videoId: Long, userId: Long): LikingState
    fun rate(videoId: Long, userId: Long, likingState: LikingState): Video
    fun getVideosByChannelId(channelId: Long, partSize: Int, partNumber: Long): List<Video>
    fun getVideosByQuery(query: String, partSize: Int, partNumber: Long): List<VideoWithChannel>
    fun uploadVideo(video: Video, cover: File? = null, source: File): Video
}