package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.*

interface VideoService {
    fun getVideoInfo(videoId: Long): Video
    fun getVideoForUser(videoId: Long, userId: Long): VideoWithUser
    fun checkVideoLikeState(videoId: Long, userId: String): LikingState
    fun rate(videoId: Long, userId: String, likingState: LikingState): Video
    fun getVideosByChannelId(channelId: Long, partSize: Int, partNumber: Long): List<Video>
    fun getVideosByQuery(query: String, partSize: Int, partNumber: Long): List<VideoWithChannel>
}