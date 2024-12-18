package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.ExtendedVideoInfo
import mikhail.shell.video.hosting.domain.VideoInfo

interface VideoService {
    fun getVideoInfo(videoId: Long): VideoInfo
    fun getExtendedVideoInfo(videoId: Long, userId: Long): ExtendedVideoInfo
    fun rate(videoId: Long, userId: Long, liking: Boolean): ExtendedVideoInfo
}