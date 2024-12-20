package mikhail.shell.video.hosting.service

import jakarta.transaction.Transactional
import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.dto.ExtendedVideoInfo
import mikhail.shell.video.hosting.repository.ChannelRepository
import mikhail.shell.video.hosting.repository.SubscriberRepository
import mikhail.shell.video.hosting.repository.UserLikeVideoRepository
import mikhail.shell.video.hosting.repository.VideoRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class VideoServiceImpl @Autowired constructor(
    private val videoRepository: VideoRepository,
    private val channelRepository: ChannelRepository,
    private val userLikeVideoRepository: UserLikeVideoRepository,
    private val subscriberRepository: SubscriberRepository
) : VideoService {
    override fun getVideoInfo(videoId: Long): VideoInfo {
        return videoRepository.findById(videoId).orElseThrow()
    }

    override fun getExtendedVideoInfo(videoId: Long, userId: Long): ExtendedVideoInfo {
        val id = UserLikeVideoId(userId, videoId)
        val liking = if (userLikeVideoRepository.existsById(id))
            userLikeVideoRepository.findById(id).orElseThrow().liking
        else
            null
        return ExtendedVideoInfo(videoRepository.findById(videoId).orElseThrow(), liking)
    }

    @Transactional
    override fun rate(videoId: Long, userId: Long, liking: Boolean): ExtendedVideoInfo {
        val id = UserLikeVideoId(userId, videoId)
        val videoInfo = videoRepository.findById(videoId).orElseThrow()
        if (!userLikeVideoRepository.existsById(id)) {
            userLikeVideoRepository.save(UserLikeVideo(id, liking))
            if (liking) {
                videoRepository.save(videoInfo.copy(likes = videoInfo.likes + 1))
            } else {
                videoRepository.save(videoInfo.copy(dislikes = videoInfo.dislikes + 1))
            }
        } else {
            val likeRow = userLikeVideoRepository.findById(id).get()
            if (likeRow.liking) {
                if (liking) {
                    userLikeVideoRepository.deleteById(id)
                } else {
                    videoRepository.save(videoInfo.copy(dislikes = videoInfo.dislikes + 1))
                    userLikeVideoRepository.save(UserLikeVideo(id, false))
                }
                videoRepository.save(videoInfo.copy(likes = videoInfo.likes - 1))
            } else {
                if (!liking) {
                    userLikeVideoRepository.deleteById(id)
                } else {
                    videoRepository.save(videoInfo.copy(likes = videoInfo.likes + 1))
                    userLikeVideoRepository.save(UserLikeVideo(id, true))
                }
                videoRepository.save(videoInfo.copy(dislikes = videoInfo.dislikes - 1))
            }
        }
        return getExtendedVideoInfo(videoId, userId)
    }

    override fun getVideosByChannelId(
        channelId: Long,
        partSize: Int,
        partNumber: Long
    ): List<VideoInfo> {
        return videoRepository.findByChannelId(
            channelId,
            PageRequest.of(
                partNumber.toInt() - 1,
                partSize
            )
        )
    }
}