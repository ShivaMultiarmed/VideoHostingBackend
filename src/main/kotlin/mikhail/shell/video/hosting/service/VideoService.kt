package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.LikingState
import mikhail.shell.video.hosting.domain.VideoInfo

interface VideoService {
    fun getVideoInfo(videoId: Long): VideoInfo
    fun checkVideoLikeState(videoId: Long, userId: String): LikingState
    fun rate(videoId: Long, userId: String, likingState: LikingState): VideoInfo
    fun getVideosByChannelId(channelId: Long, partSize: Int, partNumber: Long): List<VideoInfo>
}