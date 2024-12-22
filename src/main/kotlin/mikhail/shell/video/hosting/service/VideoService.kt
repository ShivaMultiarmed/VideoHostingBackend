package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.LikingState
import mikhail.shell.video.hosting.dto.VideoDto
import mikhail.shell.video.hosting.domain.VideoInfo

interface VideoService {
    fun getVideoInfo(videoId: Long): VideoInfo
    fun checkVideoLikeState(videoId: Long, userId: Long): LikingState
    fun rate(videoId: Long, userId: Long, likingState: LikingState): LikingState
    fun getVideosByChannelId(channelId: Long, partSize: Int, partNumber: Long): List<VideoInfo>
}