package mikhail.shell.video.hosting.service

import co.elastic.clients.elasticsearch._types.Script
import co.elastic.clients.elasticsearch._types.ScriptSort
import co.elastic.clients.elasticsearch._types.ScriptSortType
import co.elastic.clients.elasticsearch._types.SortOrder
import co.elastic.clients.elasticsearch._types.query_dsl.Query
import co.elastic.clients.json.JsonData
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import jakarta.transaction.Transactional
import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.domain.ApplicationPaths.VIDEOS_PLAYABLES_BASE_PATH
import mikhail.shell.video.hosting.domain.ApplicationPaths.VIDEOS_COVERS_BASE_PATH
import mikhail.shell.video.hosting.elastic.repository.VideoSearchRepository
import mikhail.shell.video.hosting.errors.*
import mikhail.shell.video.hosting.repository.UserLikeVideoRepository
import mikhail.shell.video.hosting.repository.UserRepository
import mikhail.shell.video.hosting.repository.VideoRepository
import mikhail.shell.video.hosting.repository.VideoWithChannelsRepository
import mikhail.shell.video.hosting.repository.entities.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.PageRequest
import org.springframework.data.elasticsearch.client.elc.NativeQuery
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.search
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.time.LocalDateTime

@Service
class VideoServiceWithDB @Autowired constructor(
    private val recommendationWeights: RecommendationWeights,
    @Qualifier("videoRepository_mysql")
    private val videoRepository: VideoRepository,
    private val videoWithChannelsRepository: VideoWithChannelsRepository,
    @Qualifier("videoRepository_elastic")
    private val videoSearchRepository: VideoSearchRepository,
    private val elasticSearchOperations: ElasticsearchOperations,
    private val userRepository: UserRepository,
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
        val sortingScriptStringified = """
            def secs = doc['dateTime'].value.toInstant().toEpochMilli() / 1000;
            return params.dateTime * secs
             + params.views * doc['views'].value 
             + params.likes * doc['likes'].value 
             + params.dislikes * doc['dislikes'].value;
        """.trimIndent()
        val sortingScript = Script.Builder()
            .lang("painless")
            .source(sortingScriptStringified)
            .params(
                mapOf(
                    "dateTime" to JsonData.of(recommendationWeights.dateTime),
                    "views" to JsonData.of(recommendationWeights.views),
                    "likes" to JsonData.of(recommendationWeights.likes),
                    "dislikes" to JsonData.of(recommendationWeights.dislikes)
                )
            )
            .build()
        val scriptSortBuilder = ScriptSort.Builder()
            .script(sortingScript)
            .order(SortOrder.Desc)
            .type(ScriptSortType.Number)
            .build()
            ._toSortOptions()
        val searchQuery = NativeQuery.builder()
            .withQuery(
                Query.of {
                    it.match { aMatch ->
                        aMatch
                            .field("title")
                            .query(query)
                    }
                }
            )
            .withSort(scriptSortBuilder)
            .build()
        val ids = elasticSearchOperations.search<VideoEntity>(searchQuery).map { it.content.videoId }
        return videoWithChannelsRepository.findAllById(ids).map { it.toDomain() }
    }

    override fun uploadVideo(
        video: Video,
        cover: mikhail.shell.video.hosting.domain.File?,
        source: mikhail.shell.video.hosting.domain.File
    ): Video {
        val addedVideo = saveVideoDetails(video)
        saveVideoSource(addedVideo.videoId!!, source)
        cover?.let {
            saveVideoCover(addedVideo.videoId, it)
        }
        confirmVideoUpload(addedVideo.videoId)
        return addedVideo
    }

    override fun saveVideoDetails(video: Video): Video {
        val compoundError = CompoundError<UploadVideoError>()
        if (video.channelId <= 0) {
            compoundError.add(UploadVideoError.CHANNEL_NOT_VALID)
        }
        if (video.title.length > ValidationRules.MAX_TITLE_LENGTH) {
            compoundError.add(UploadVideoError.TITLE_TOO_LARGE)
        }
        if (compoundError.isNotNull()) {
            throw HostingDataException(compoundError)
        }
        val videoEntityToAdd = video
            .toEntity()
            .copy(
                videoId = null,
                views = 0,
                likes = 0,
                dislikes = 0,
                dateTime = LocalDateTime.now(),
                state = VideoState.CREATED
            )
        val addedVideoEntity = videoRepository.save(videoEntityToAdd)
        videoSearchRepository.save(addedVideoEntity)
        return addedVideoEntity.toDomain()
    }

    override fun saveVideoSource(videoId: Long, source: mikhail.shell.video.hosting.domain.File): Boolean {
        val compoundError = CompoundError<UploadVideoError>()
        if (source.content?.isEmpty() != false) {
            compoundError.add(UploadVideoError.SOURCE_EMPTY)
        } else if (!source.mimeType!!.contains("video")) {
            compoundError.add(UploadVideoError.SOURCE_TYPE_NOT_VALID)
        } else if (source.content.size > ValidationRules.MAX_VIDEO_SIZE) {
            compoundError.add(UploadVideoError.SOURCE_TOO_LARGE)
        }
        if (compoundError.isNotNull()) {
            throw HostingDataException(compoundError)
        }
        return saveFile(
            input = source.content!!.inputStream(),
            path = "$VIDEOS_PLAYABLES_BASE_PATH/$videoId.${source.name!!.parseExtension()}"
        )
    }

    override fun saveVideoCover(videoId: Long, cover: mikhail.shell.video.hosting.domain.File): Boolean {
        val compoundError = CompoundError<UploadVideoError>()
        if (cover.content?.isEmpty() != false) {
            compoundError.add(UploadVideoError.COVER_EMPTY)
        } else if (!cover.mimeType!!.contains("image")) {
            compoundError.add(UploadVideoError.COVER_TYPE_NOT_VALID)
        } else if (cover.content.size > ValidationRules.MAX_IMAGE_SIZE) {
            compoundError.add(UploadVideoError.COVER_TOO_LARGE)
        }
        if (compoundError.isNotNull()) {
            throw HostingDataException(compoundError)
        }
        return saveFile(
            input = cover.content!!.inputStream(),
            path = "$VIDEOS_COVERS_BASE_PATH/$videoId.${cover.name!!.parseExtension()}"
        )
    }

    override fun confirmVideoUpload(videoId: Long): Boolean {
        val videoEntityToConfirm = videoRepository
            .findById(videoId)
            .get()
            .copy(
                state = VideoState.UPLOADED,
                dateTime = LocalDateTime.now()
            )
        videoRepository.save(videoEntityToConfirm)
        videoSearchRepository.save(videoEntityToConfirm)
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

    override fun getRecommendedVideos(userId: Long, partIndex: Long, partSize: Int): List<VideoWithChannel> {
        if (!userRepository.existsById(userId)) {
            throw HostingDataException(VideoRecommendationsLoadingError.USER_ID_NOT_FOUND)
        }
        return videoWithChannelsRepository
            .findRecommendedVideos(
                userId,
                recommendationWeights.dateTime,
                recommendationWeights.subscribers,
                recommendationWeights.views,
                recommendationWeights.likes,
                recommendationWeights.dislikes,
                PageRequest.of(partIndex.toInt(), partSize)
            ).map {
                it.toDomain()
            }.toList()
    }

    private fun saveFile(input: InputStream, path: String): Boolean {
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
        findFileByName(File(VIDEOS_PLAYABLES_BASE_PATH), videoId.toString())?.delete()
        findFileByName(File(VIDEOS_COVERS_BASE_PATH), videoId.toString())?.delete()
        return !videoRepository.existsById(videoId)
    }

    override fun editVideo(
        video: Video,
        coverAction: EditAction,
        cover: mikhail.shell.video.hosting.domain.File?
    ): Video {
        val compoundError = CompoundError<EditVideoError>()
        if (video.title.length > ValidationRules.MAX_TITLE_LENGTH) {
            compoundError.add(EditVideoError.TITLE_TOO_LARGE)
        }
        cover?.let {
            if (it.content!!.isEmpty()) {
                compoundError.add(EditVideoError.COVER_EMPTY)
            } else if (it.content.size > ValidationRules.MAX_IMAGE_SIZE) {
                compoundError.add(EditVideoError.COVER_TOO_LARGE)
            }
            if (!it.mimeType!!.contains("image")) {
                compoundError.add(EditVideoError.COVER_TYPE_NOT_VALID)
            }
        }
        if (compoundError.isNotNull()) {
            throw HostingDataException(compoundError)
        }
        val videoEntityToEdit = videoRepository
            .findById(video.videoId!!)
            .get()
            .copy(
                title = video.title
            )
        val updatedVideoEntity = videoRepository.save(videoEntityToEdit)
        videoSearchRepository.save(updatedVideoEntity)
        val coverDir = File(VIDEOS_COVERS_BASE_PATH)
        if (coverAction != EditAction.KEEP) {
            val coverFile = coverDir.listFiles()?.firstOrNull { it.nameWithoutExtension == video.videoId.toString() }
            coverFile?.delete()
        }
        if (cover != null) {
            val coverExtension = cover.name?.parseExtension()
            File("$VIDEOS_COVERS_BASE_PATH/${updatedVideoEntity.videoId}.$coverExtension").writeBytes(cover.content!!)
        }
        return updatedVideoEntity.toDomain()
    }

    override fun sync() {
        videoSearchRepository.deleteAll()
        val videos = videoRepository.findAll()
        videoSearchRepository.saveAll(videos)
    }

    companion object {
        private const val TRANSFER_BUFFER_SIZE = 40 * 1024 * 1024
        private const val IO_BUFFER_SIZE = 100 * 1024
    }
}
