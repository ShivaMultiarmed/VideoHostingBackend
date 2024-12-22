package mikhail.shell.video.hosting.service

import jakarta.transaction.Transactional
import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.dto.VideoDto
import mikhail.shell.video.hosting.repository.ChannelRepository
import mikhail.shell.video.hosting.repository.SubscriberRepository
import mikhail.shell.video.hosting.repository.UserLikeVideoRepository
import mikhail.shell.video.hosting.repository.VideoRepository
import mikhail.shell.video.hosting.repository.models.UserLikeVideo
import mikhail.shell.video.hosting.repository.models.UserLikeVideoId
import mikhail.shell.video.hosting.repository.models.toDomain
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class VideoServiceImpl @Autowired constructor(
    private val videoRepository: VideoRepository,
    private val userLikeVideoRepository: UserLikeVideoRepository
) : VideoService {
    override fun getVideoInfo(videoId: Long): VideoInfo {
        return videoRepository.findById(videoId).orElseThrow().toDomain()
    }

    override fun checkVideoLikeState(videoId: Long, userId: Long): LikingState {
        val id = UserLikeVideoId(userId, videoId)
        return userLikeVideoRepository.findById(id).orElse(null)?.liking?: LikingState.NONE
    }

    @Transactional
    override fun rate(videoId: Long, userId: Long, likingState: LikingState): VideoInfo {
        val id = UserLikeVideoId(userId, videoId)
        val previousLikingState = checkVideoLikeState(videoId, userId)
        val videoEntity = videoRepository.findById(videoId).orElseThrow()
        if (likingState != LikingState.NONE) {
            userLikeVideoRepository.save(UserLikeVideo(id, likingState))
        } else if (userLikeVideoRepository.existsById(id)) {
            userLikeVideoRepository.deleteById(id)
        }
        var newLikes = videoEntity.likes
        var newDislikes = videoEntity.dislikes
        if (likingState == LikingState.LIKED) {
            if (previousLikingState != LikingState.LIKED)
                newLikes += 1
            if (previousLikingState == LikingState.DISLIKED)
                newDislikes -= 1
        } else if (likingState == LikingState.DISLIKED) {
            if (previousLikingState != LikingState.DISLIKED)
                newDislikes += 1
            if (previousLikingState == LikingState.LIKED)
                newLikes -= 1
        } else {
            if (previousLikingState == LikingState.LIKED) {
                newLikes -= 1
            }
            if (previousLikingState == LikingState.DISLIKED) {
                newDislikes -= 1
            }
        }
        videoRepository.save(videoEntity.copy(likes = newLikes, dislikes = newDislikes))
        return videoRepository.findById(videoId).orElseThrow().toDomain()
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
        ).map {
            it.toDomain()
        }
    }
}