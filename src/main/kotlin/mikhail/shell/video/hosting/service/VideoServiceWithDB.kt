package mikhail.shell.video.hosting.service

import jakarta.transaction.Transactional
import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.domain.ApplicationPaths.VIDEOS_PLAYABLES_BASE_PATH
import mikhail.shell.video.hosting.domain.ApplicationPaths.VIDEOS_COVERS_BASE_PATH
import mikhail.shell.video.hosting.elastic.repository.VideoSearchRepository
import mikhail.shell.video.hosting.repository.UserLikeVideoRepository
import mikhail.shell.video.hosting.repository.VideoRepository
import mikhail.shell.video.hosting.repository.VideoWithChannelsRepository
import mikhail.shell.video.hosting.repository.models.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.io.File

@Service
class VideoServiceWithDB @Autowired constructor(
    @Qualifier("videoRepository_mysql")
    private val videoRepository: VideoRepository,
    private val videoWithChannelsRepository: VideoWithChannelsRepository,
    @Qualifier("videoRepository_elastic")
    private val videoSearchRepository: VideoSearchRepository,
    private val userLikeVideoRepository: UserLikeVideoRepository
) : VideoService {

    override fun getVideoInfo(videoId: Long): Video {
        return videoRepository.findById(videoId).orElseThrow().toDomain()
    }

    override fun getVideoForUser(videoId: Long, userId: Long): VideoWithUser {
        val v = videoRepository.findById(videoId).orElseThrow()
        val likingId = UserLikeVideoId(userId, videoId)

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
        return userLikeVideoRepository.findById(id).orElse(null)?.likingState ?: LikingState.NONE
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
            videoEntity.copy(
                likes = newLikes,
                dislikes = newDislikes,
            )
        )
        videoSearchRepository.save(
            videoEntity.copy(
                likes = newLikes,
                dislikes = newDislikes,
            )
        )
        return videoRepository.findById(videoId).orElseThrow().toDomain()
    }

    override fun getVideosByChannelId(
        channelId: Long,
        partSize: Int,
        partNumber: Long
    ): List<Video> {
        return videoRepository.findByChannelIdOrderByDateTimeDesc(
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
        val ids = videoSearchRepository.findByTitle(query).map { it.videoId }
        return videoWithChannelsRepository.findAllById(ids).map { it.toDomain() }
    }

    override fun uploadVideo(
        video: Video,
        cover: mikhail.shell.video.hosting.domain.File?,
        source: mikhail.shell.video.hosting.domain.File
    ): Video {
        val addedVideo = videoRepository.save(video.toEntity()).toDomain()
        videoSearchRepository.save(addedVideo.toEntity())
        val sourceExtension = source.name?.parseExtension()
        source.content?.let {
            File("$VIDEOS_PLAYABLES_BASE_PATH/${addedVideo.videoId}.$sourceExtension").writeBytes(it)
        }
        if (cover != null) {
            val coverExtension = cover.name?.parseExtension()
            File("$VIDEOS_COVERS_BASE_PATH/${addedVideo.videoId}.$coverExtension").writeBytes(cover.content!!)
        }
        return addedVideo
    }

    override fun incrementViews(videoId: Long): Long {
        val video = videoRepository.findById(videoId).orElseThrow()
        videoSearchRepository.save(video.copy(views = video.views + 1))
        return videoRepository.save(video.copy(views = video.views + 1)).views
    }

    override fun deleteVideo(videoId: Long): Boolean {
        videoRepository.deleteById(videoId)
        videoRepository.deleteById(videoId)
        return !videoRepository.existsById(videoId)
    }

    override fun editVideo(
        video: Video,
        coverAction: EditAction,
        cover: mikhail.shell.video.hosting.domain.File?
    ): Video {
        val updatedVideo = videoRepository.save(video.toEntity()).toDomain()
        videoSearchRepository.save(updatedVideo.toEntity())
        val coverDir = File(VIDEOS_COVERS_BASE_PATH)
        if (coverAction != EditAction.KEEP) {
            val coverFile = coverDir.listFiles()?.firstOrNull { it.nameWithoutExtension == video.videoId.toString() }
            coverFile?.delete()
        }
        if (cover != null) {
            val coverExtension = cover.name?.parseExtension()
            File("$VIDEOS_COVERS_BASE_PATH/${updatedVideo.videoId}.$coverExtension").writeBytes(cover.content!!)
        }
        return updatedVideo
    }

    override fun sync() {
        val videos = videoRepository.findAll()
        videoSearchRepository.saveAll(videos)
    }
}