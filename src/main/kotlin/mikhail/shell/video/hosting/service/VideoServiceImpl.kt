package mikhail.shell.video.hosting.service

import jakarta.transaction.Transactional
import mikhail.shell.video.hosting.dto.ExtendedVideoInfo
import mikhail.shell.video.hosting.domain.UserLikeVideo
import mikhail.shell.video.hosting.domain.UserLikeVideoId
import mikhail.shell.video.hosting.domain.VideoInfo
import mikhail.shell.video.hosting.repository.UserLikeVideoRepository
import mikhail.shell.video.hosting.repository.VideoRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class VideoServiceImpl @Autowired constructor(
    private val videoRepository: VideoRepository,
    private val userLikeVideoRepository: UserLikeVideoRepository
) : VideoService {
    override fun getVideoInfo(videoId: Long): VideoInfo {
        return videoRepository.findById(videoId).orElseThrow()
    }

    override fun getExtendedVideoInfo(videoId: Long, userId: Long): ExtendedVideoInfo {
        val id = UserLikeVideoId(userId, videoId)
        return if (userLikeVideoRepository.existsById(id))
            videoRepository.findByVideoIdAndUserId(videoId, userId).orElseThrow()
        else
            ExtendedVideoInfo(videoRepository.findById(videoId).orElseThrow(), null)
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
}