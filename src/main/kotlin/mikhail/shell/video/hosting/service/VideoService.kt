package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.LikingState
import mikhail.shell.video.hosting.domain.VideoDetails
import mikhail.shell.video.hosting.domain.Video
import mikhail.shell.video.hosting.domain.VideoWithChannel

interface VideoService {
    fun getVideoInfo(videoId: Long): Video
    fun checkVideoLikeState(videoId: Long, userId: String): LikingState
    fun rate(videoId: Long, userId: String, likingState: LikingState): Video
    fun getVideosByChannelId(channelId: Long, partSize: Int, partNumber: Long): List<Video>
    fun getVideosByQuery(query: String, partSize: Int, partNumber: Long): List<VideoWithChannel>
}