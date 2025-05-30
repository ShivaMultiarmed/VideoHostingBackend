package mikhail.shell.video.hosting.service

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import jakarta.transaction.Transactional
import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.domain.ApplicationPaths.VIDEOS_PLAYABLES_BASE_PATH
import mikhail.shell.video.hosting.domain.ApplicationPaths.VIDEOS_COVERS_BASE_PATH
import mikhail.shell.video.hosting.elastic.repository.VideoSearchRepository
import mikhail.shell.video.hosting.repository.UserLikeVideoRepository
import mikhail.shell.video.hosting.repository.VideoRepository
import mikhail.shell.video.hosting.repository.VideoWithChannelsRepository
import mikhail.shell.video.hosting.repository.entities.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@Service
class VideoServiceWithDB @Autowired constructor(
    @Qualifier("videoRepository_mysql")
    private val videoRepository: VideoRepository,
    private val videoWithChannelsRepository: VideoWithChannelsRepository,
    @Qualifier("videoRepository_elastic")
    private val videoSearchRepository: VideoSearchRepository,
    private val userLikeVideoRepository: UserLikeVideoRepository,
    private val fcm: FirebaseMessaging
) : VideoService {

    private val CHANNELS_TOPICS_PREFIX = "channels"

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
        return videoRepository.findByChannelIdAndStateOrderByDateTimeDesc(
            channelId = channelId,
            pageable = PageRequest.of(
                partNumber.toInt(),
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
        val ids = videoSearchRepository.findByTitleAndState(
            title = query,
            pageable = PageRequest.of(
                partNumber.toInt(),
                partSize
            )
        ).map { it.videoId }
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
        val videoWithChannel = videoWithChannelsRepository.findById(addedVideo.videoId!!).get()
        val message = Message.builder()
            .setTopic("$CHANNELS_TOPICS_PREFIX.${video.channelId}")
            .putAllData(
                mapOf(
                    "channelTitle" to videoWithChannel.channel.title,
                    "videoTitle" to videoWithChannel.title,
                    "videoId" to videoWithChannel.videoId.toString()
                )
            ).build()
        fcm.send(message)
        confirmVideoUpload(addedVideo.videoId)
        return addedVideo
    }

    override fun saveVideoDetails(video: Video): Video {
        val addedVideo = videoRepository.save(video.toEntity()).toDomain()
        videoSearchRepository.save(addedVideo.toEntity())
        return addedVideo
    }

    override fun saveVideoSource(videoId: Long, extension: String, input: InputStream, chunkNumber: Int): Boolean {
        return saveFile(
            input = input,
            path = "$VIDEOS_PLAYABLES_BASE_PATH/$videoId.$extension",
            chunkNumber = chunkNumber
        )
    }

    override fun saveVideoCover(videoId: Long, extension: String, input: InputStream): Boolean {
        return saveFile(
            input = input,
            path = "$VIDEOS_COVERS_BASE_PATH/$videoId.$extension"
        )
    }

    override fun confirmVideoUpload(videoId: Long): Boolean {
        var videoEntity = videoRepository.findById(videoId).get()
        videoEntity = videoEntity.copy(state = VideoState.UPLOADED)
        videoRepository.save(videoEntity)
        videoSearchRepository.save(videoEntity)
        val videoWithChannel = videoWithChannelsRepository.findById(videoId).get()
        val message = Message.builder()
            .setTopic("$CHANNELS_TOPICS_PREFIX.${videoWithChannel.channelId}")
            .putAllData(
                mapOf(
                    "channelTitle" to videoWithChannel.channel.title,
                    "videoTitle" to videoWithChannel.title,
                    "videoId" to videoWithChannel.videoId.toString()
                )
            ).build()
        fcm.send(message)
        return true
    }

    override fun checkOwner(userId: Long, videoId: Long): Boolean {
        return videoWithChannelsRepository.existsByChannel_OwnerIdAndVideoId(userId, videoId)
    }

    private fun saveFile(input: InputStream, path: String, chunkNumber: Int = 0): Boolean {
        input.use { inStream ->
            val output = FileOutputStream(File(path), true)
            output.use { outStream ->
                val buffer = ByteArray(IO_BUFFER_SIZE)
                var bytesRead: Int
                while (inStream.read(buffer).also { bytesRead = it } != -1) {
                    outStream.write(buffer, 0, bytesRead)
                }
            }
        }
        return true
    }

    override fun incrementViews(videoId: Long): Long {
        val video = videoRepository.findById(videoId).orElseThrow()
        videoSearchRepository.save(video.copy(views = video.views + 1))
        return videoRepository.save(video.copy(views = video.views + 1)).views
    }

    override fun deleteVideo(videoId: Long): Boolean {
        videoRepository.deleteById(videoId)
        videoSearchRepository.deleteById(videoId)
        return !videoRepository.existsById(videoId)
    }

    override fun editVideo(
        video: Video,
        coverAction: EditAction,
        cover: mikhail.shell.video.hosting.domain.File?
    ): Video {
        val videoState = videoRepository.findById(video.videoId!!).get().state
        val updatedVideo = videoRepository.save(video.toEntity(videoState)).toDomain()
        videoSearchRepository.save(updatedVideo.toEntity(videoState))
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

    companion object {
        private const val TRANSFER_BUFFER_SIZE = 40 * 1024 * 1024
        private const val IO_BUFFER_SIZE = 100 * 1024
    }
}
