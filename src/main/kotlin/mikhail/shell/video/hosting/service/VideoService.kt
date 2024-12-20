package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.ChannelInfo
import mikhail.shell.video.hosting.domain.VideoDetails
import mikhail.shell.video.hosting.dto.ExtendedVideoInfo
import mikhail.shell.video.hosting.domain.VideoInfo

interface VideoService {
    fun getVideoInfo(videoId: Long): VideoInfo
    fun getExtendedVideoInfo(videoId: Long, userId: Long): ExtendedVideoInfo
    fun rate(videoId: Long, userId: Long, liking: Boolean): ExtendedVideoInfo
    fun getVideosByChannelId(channelId: Long, partSize: Int, partNumber: Long): List<VideoInfo>
}