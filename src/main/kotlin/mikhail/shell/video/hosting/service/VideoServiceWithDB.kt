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
import mikhail.shell.video.hosting.elastic.documents.toDocument
import mikhail.shell.video.hosting.elastic.repository.VideoSearchRepository
import mikhail.shell.video.hosting.errors.*
import mikhail.shell.video.hosting.repository.*
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
import java.nio.file.Paths
import java.time.Instant

@Service
class VideoServiceWithDB @Autowired constructor(
    private val recommendationWeights: RecommendationWeights,
    private val videoRepository: VideoRepository,
    private val videoWithChannelsRepository: VideoWithChannelsRepository,
    private val videoSearchRepository: VideoSearchRepository,
    private val elasticSearchOperations: ElasticsearchOperations,
    private val userRepository: UserRepository,
    private val userLikeVideoRepository: UserLikeVideoRepository,
    private val channelRepository: ChannelRepository,
    private val fcm: FirebaseMessaging
) : VideoService {

    override fun getVideoInfo(videoId: Long): Video {
        return videoRepository.findById(videoId).orElseThrow().toDomain()
    }

    override fun getVideoForUser(videoId: Long, userId: Long): VideoWithUser {
        if (!videoRepository.existsById(videoId)) {
            throw NoSuchElementException()
        }
        val likingId = VideoLikingId(userId, videoId)

        val liking = if (!userLikeVideoRepository.existsById(likingId)) Liking.NONE
        else userLikeVideoRepository.findById(likingId).get().liking

        return videoRepository.findById(videoId).get().toDomain() with liking
    }

    override fun checkExistence(videoId: Long): Boolean {
        return videoRepository.existsById(videoId)
    }

    override fun checkVideoLikeState(videoId: Long, userId: Long): Liking {
        val id = VideoLikingId(userId, videoId)
        return userLikeVideoRepository.findById(id).orElse(null)?.liking ?: Liking.NONE
    }

    @Transactional
    override fun rate(videoId: Long, userId: Long, liking: Liking): VideoWithUser {
        val id = VideoLikingId(userId, videoId)
        val previousLiking = checkVideoLikeState(videoId, userId)
        val videoEntity = videoRepository.findById(videoId).orElseThrow()
        if (liking != Liking.NONE) {
            userLikeVideoRepository.save(VideoLiking(id, liking))
        } else if (userLikeVideoRepository.existsById(id)) {
            userLikeVideoRepository.deleteById(id)
        }
        var newLikes = videoEntity.likes
        var newDislikes = videoEntity.dislikes
        if (liking == Liking.LIKED) {
            if (previousLiking != Liking.LIKED)
                newLikes += 1
            if (previousLiking == Liking.DISLIKED)
                newDislikes -= 1
        } else if (liking == Liking.DISLIKED) {
            if (previousLiking != Liking.DISLIKED)
                newDislikes += 1
            if (previousLiking == Liking.LIKED)
                newLikes -= 1
        } else {
            if (previousLiking == Liking.LIKED) {
                newLikes -= 1
            }
            if (previousLiking == Liking.DISLIKED) {
                newDislikes -= 1
            }
        }
        val videoEntityToSave = videoEntity.copy(
            likes = newLikes,
            dislikes = newDislikes,
        )
        videoRepository.save(videoEntityToSave)
        videoSearchRepository.save(videoEntityToSave.toDocument())
        return getVideoForUser(videoId, userId)
    }

    override fun getVideosByChannelId(
        channelId: Long,
        partSize: Int,
        partNumber: Long
    ): List<Video> {
        if (!channelRepository.existsById(channelId)) {
            throw NoSuchElementException()
        }
        return videoRepository.findByChannelIdAndStateOrderByDateTimeDesc(
            channelId = channelId,
            pageable = PageRequest.of(
                partNumber.toInt(),
                partSize
            )
        ).map { it.toDomain() }
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

    override fun saveVideoDetails(video: Video): Video {
        val compoundError = CompoundError<UploadVideoError>()
        if (!channelRepository.existsById(video.channelId)) {
            compoundError.add(UploadVideoError.CHANNEL_NOT_VALID)
        }
        if (video.title.length > ValidationRules.MAX_TITLE_LENGTH) {
            compoundError.add(UploadVideoError.TITLE_TOO_LARGE)
        }
        if (compoundError.isNotEmpty()) {
            throw ValidationException(compoundError)
        }
        val videoEntityToAdd = video
            .toEntity()
            .copy(
                videoId = null,
                views = 0,
                likes = 0,
                dislikes = 0,
                dateTime = Instant.now(),
                state = VideoState.CREATED
            )
        val addedVideoEntity = videoRepository.save(videoEntityToAdd)
        videoSearchRepository.save(addedVideoEntity.toDocument())
        return addedVideoEntity.toDomain()
    }

    override fun saveVideoSource(videoId: Long, source: mikhail.shell.video.hosting.domain.File): Boolean {
        if (!videoRepository.existsById(videoId)) {
            throw NoSuchElementException()
        }
        val compoundError = CompoundError<UploadVideoError>()
        if (source.content?.isEmpty() != false) {
            compoundError.add(UploadVideoError.SOURCE_EMPTY)
        } else if (!source.mimeType!!.contains("video")) {
            compoundError.add(UploadVideoError.SOURCE_TYPE_NOT_VALID)
        } else if (source.content.size > ValidationRules.MAX_VIDEO_SIZE) {
            compoundError.add(UploadVideoError.SOURCE_TOO_LARGE)
        }
        if (compoundError.isNotEmpty()) {
            throw ValidationException(compoundError)
        }
        return saveFile(
            input = source.content!!.inputStream(),
            path = "$VIDEOS_PLAYABLES_BASE_PATH/$videoId.${source.name!!.parseExtension()}"
        )
    }

    override fun saveVideoCover(videoId: Long, cover: UploadedFile): Boolean {
        if (!videoRepository.existsById(videoId)) {
            throw NoSuchElementException()
        }
        val compoundError = CompoundError<UploadVideoError>()
        if (!cover.mimeType.contains("image")) {
            compoundError.add(UploadVideoError.COVER_TYPE_NOT_VALID)
        }
        val imageBytes = cover.inputStream.use { it.readBytes() }
        if (imageBytes.size > ValidationRules.MAX_IMAGE_SIZE) {
            compoundError.add(UploadVideoError.COVER_TOO_LARGE)
        }
        if (compoundError.isNotEmpty()) {
            throw ValidationException(compoundError)
        }
        return uploadImage(
            uploadedFile = cover.copy(
                inputStream = imageBytes.inputStream()
            ),
            targetFile = "$VIDEOS_COVERS_BASE_PATH/$videoId.jpg",
            width = 500,
            height = 280
        )
    }

    override fun confirmVideoUpload(videoId: Long): Boolean {
        if (!videoRepository.existsById(videoId)) {
            throw NoSuchElementException()
        }
        val videoEntityToConfirm = videoRepository
            .findById(videoId)
            .get()
            .copy(
                state = VideoState.UPLOADED,
                dateTime = Instant.now()
            )
        videoRepository.save(videoEntityToConfirm)
        videoSearchRepository.save(videoEntityToConfirm.toDocument())
        val videoWithChannel = videoWithChannelsRepository.findById(videoId).get()
        val message = Message.builder()
            .setTopic("${Companion.CHANNELS_TOPICS_PREFIX}.${videoWithChannel.channelId}")
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
            throw NoSuchElementException()
        }
        return videoWithChannelsRepository
            .findRecommendedVideos(
                userId = userId,
                dateTimeWeight = recommendationWeights.dateTime,
                subscribersWeight = recommendationWeights.subscribers,
                viewsWeight = recommendationWeights.views,
                likesWeight = recommendationWeights.likes,
                dislikesWeight = recommendationWeights.dislikes,
                pageable = PageRequest.of(partIndex.toInt(), partSize)
            ).map { it.toDomain() }
            .toList()
    }

    override fun sync() {
        videoRepository
            .findAll()
            .map { it.toDocument() }
            .let {
                videoSearchRepository.deleteAll()
                videoSearchRepository.saveAll(it)
            }
    }

    private fun saveFile(input: InputStream, path: String): Boolean {
        return try {
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
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun incrementViews(videoId: Long): Video {
        val video = videoRepository.findById(videoId).orElseThrow()
        videoRepository.save(video.copy(views = video.views + 1))
        videoSearchRepository.save(video.toDocument().copy(views = video.views + 1))
        return videoRepository.findById(videoId).get().toDomain()
    }

    override fun deleteVideo(videoId: Long): Boolean {
        if (!videoRepository.existsById(videoId)) {
            throw NoSuchElementException()
        }
        videoRepository.deleteById(videoId)
        videoSearchRepository.deleteById(videoId)
        findFileByName(File(VIDEOS_PLAYABLES_BASE_PATH), videoId.toString())?.delete()
        findFileByName(File(VIDEOS_COVERS_BASE_PATH), videoId.toString())?.delete()
        return !videoRepository.existsById(videoId)
    }

    override fun editVideo(
        video: Video,
        coverAction: EditAction,
        cover: UploadedFile?
    ): Video {
        val compoundError = CompoundError<EditVideoError>()
        if (video.title.length > ValidationRules.MAX_TITLE_LENGTH) {
            compoundError.add(EditVideoError.TITLE_TOO_LARGE)
        }
        if (compoundError.isNotEmpty()) {
            throw ValidationException(compoundError)
        }
        val videoEntityToEdit = videoRepository
            .findById(video.videoId!!)
            .get()
            .copy(
                title = video.title
            )
        val updatedVideoEntity = videoRepository.save(videoEntityToEdit)
        videoSearchRepository.save(updatedVideoEntity.toDocument())
        if (coverAction != EditAction.KEEP) {
            findFileByName(Paths.get(VIDEOS_COVERS_BASE_PATH).toFile(), video.videoId.toString())?.delete()
        }
        if (cover != null) {
            uploadImage(
                uploadedFile = cover,
                targetFile = "$VIDEOS_COVERS_BASE_PATH/${updatedVideoEntity.videoId}.jpg",
                width = 500,
                height = 280
            )
        }
        return updatedVideoEntity.toDomain()
    }

    private companion object {
        const val IO_BUFFER_SIZE = 100 * 1024
        const val CHANNELS_TOPICS_PREFIX = "channels"
    }
}
