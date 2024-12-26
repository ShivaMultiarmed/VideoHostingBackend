package mikhail.shell.video.hosting.service

import jakarta.transaction.Transactional
import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.repository.UserLikeVideoRepository
import mikhail.shell.video.hosting.repository.VideoRepository
import mikhail.shell.video.hosting.repository.VideoWithChannelsRepository
import mikhail.shell.video.hosting.repository.models.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.io.File

@Service
class VideoServiceWithDB @Autowired constructor(
    private val videoRepository: VideoRepository,
    private val videoWithChannelsRepository: VideoWithChannelsRepository,
    private val userLikeVideoRepository: UserLikeVideoRepository
) : VideoService {

    companion object {
        private const val VIDEOS_BASE_PATH = "D:/VideoHostingStorage/videos"
        private const val VIDEOS_PLAYABLES_BASE_PATH = "$VIDEOS_BASE_PATH/playables"
        private const val VIDEOS_COVERS_BASE_PATH = "$VIDEOS_BASE_PATH/covers"
    }

    override fun getVideoInfo(videoId: Long): Video {
        return videoRepository.findById(videoId).orElseThrow().toDomain()
    }

    override fun getVideoForUser(videoId: Long, userId: Long): VideoWithUser {
        val v = videoRepository.findById(videoId).orElseThrow()
        val likingId = UserLikeVideoId(userId,videoId)

        val liking = if (!userLikeVideoRepository.existsById(likingId))
            LikingState.NONE
        else userLikeVideoRepository.findById(likingId).orElseThrow().likingState

        return VideoWithUser(
            v.videoId,
            v.channelId,
            v.title,
            v.dateTime,
            v.views,
            v.likes,
            v.dislikes,
            liking
        )
    }

    override fun checkVideoLikeState(videoId: Long, userId: Long): LikingState {
        val id = UserLikeVideoId(userId, videoId)
        return userLikeVideoRepository.findById(id).orElse(null)?.likingState?: LikingState.NONE
    }

    @Transactional
    override fun rate(videoId: Long, userId: Long, likingState: LikingState): Video {
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
        videoRepository.save(
            VideoEntity(
                videoEntity.videoId,
                videoEntity.channelId,
                videoEntity.title,
                videoEntity.dateTime,
                videoEntity.views,
                newLikes,
                newDislikes,
            )
        )
        return videoRepository.findById(videoId).orElseThrow().toDomain()
    }

    override fun getVideosByChannelId(
        channelId: Long,
        partSize: Int,
        partNumber: Long
    ): List<Video> {
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
    override fun getVideosByQuery(
        query: String,
        partSize: Int,
        partNumber: Long
    ): List<VideoWithChannel> {
        return videoWithChannelsRepository.findByTitleLike("%$query%").map {
            it.toDomain()
        }
    }
    override fun uploadVideo(video: Video, coverContent: ByteArray?, sourceContent: ByteArray): Video {
        val addedVideo = videoRepository.save(video.toEntity()).toDomain()
        val sourceFile = File("$VIDEOS_PLAYABLES_BASE_PATH/${addedVideo.videoId}.mp4").writeBytes(sourceContent)
        val coverFile = File("$VIDEOS_COVERS_BASE_PATH/${addedVideo.videoId}.mp4").writeBytes(sourceContent)
        return addedVideo
    }
}