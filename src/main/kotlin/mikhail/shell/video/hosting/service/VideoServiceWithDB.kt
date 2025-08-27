package mikhail.shell.video.hosting.service

import co.elastic.clients.elasticsearch._types.Script
import co.elastic.clients.elasticsearch._types.ScriptSort
import co.elastic.clients.elasticsearch._types.ScriptSortType
import co.elastic.clients.elasticsearch._types.SortOrder
import co.elastic.clients.elasticsearch._types.query_dsl.Query
import co.elastic.clients.json.JsonData
import com.google.api.client.json.Json
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.gson.Gson
import jakarta.transaction.Transactional
import mikhail.shell.video.hosting.controllers.VideoMetaData
import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.domain.ApplicationPaths.TEMP_VIDEO_SOURCE_BASE_PATH
import mikhail.shell.video.hosting.domain.ApplicationPaths.VIDEOS_PLAYABLES_BASE_PATH
import mikhail.shell.video.hosting.domain.ApplicationPaths.VIDEOS_COVERS_BASE_PATH
import mikhail.shell.video.hosting.elastic.documents.toDocument
import mikhail.shell.video.hosting.elastic.repository.VideoSearchRepository
import mikhail.shell.video.hosting.entities.*
import mikhail.shell.video.hosting.errors.*
import mikhail.shell.video.hosting.repository.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.data.domain.PageRequest
import org.springframework.data.elasticsearch.client.elc.NativeQuery
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.search
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.notExists

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

    override fun get(videoId: Long): Video {
        return videoRepository.findById(videoId).orElseThrow().toDomain()
    }

    override fun get(videoId: Long, userId: Long): VideoWithUser {
        if (!videoRepository.existsById(videoId)) {
            throw NoSuchElementException()
        }
        if (!userRepository.existsById(userId)) {
            throw UnauthenticatedException()
        }
        val likingId = VideoLikingId(userId, videoId)

        val liking = if (!userLikeVideoRepository.existsById(likingId)) Liking.NONE
        else userLikeVideoRepository.findById(likingId).get().liking

        return videoRepository.findById(videoId).get().toDomain() with liking
    }

    override fun checkExistence(videoId: Long): Boolean {
        return videoRepository.existsById(videoId)
    }

    override fun checkLiking(videoId: Long, userId: Long): Liking {
        val id = VideoLikingId(userId, videoId)
        return userLikeVideoRepository.findById(id).orElse(null)?.liking ?: Liking.NONE
    }

    @Transactional
    override fun rate(videoId: Long, userId: Long, liking: Liking): VideoWithUser {
        val id = VideoLikingId(userId, videoId)
        val previousLiking = checkLiking(videoId, userId)
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
        return get(videoId, userId)
    }

    override fun getByChannelId(
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

    override fun getByQuery(
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

    override fun save(
        userId: Long,
        video: Video,
        cover: UploadedFile?
    ): Video {
        if (!channelRepository.existsByOwnerIdAndChannelId(userId = userId, channelId = video.channelId)) {
            throw IllegalAccessException()
        }
        val savedVideoEntity = videoRepository.save(video.toEntity())
        videoSearchRepository.save(savedVideoEntity.toDocument())
        cover?.let {
            uploadImage(
                uploadedFile = it,
                targetFile = "$VIDEOS_COVERS_BASE_PATH/${savedVideoEntity.videoId}.jpg",
                width = 500,
                height = 280
            )
        }
        return savedVideoEntity.toDomain()
    }

    override fun saveVideoSource(userId: Long, videoId: Long, chunkIndex: Long, source: InputStream): Boolean {
        if (!videoRepository.existsById(videoId)) {
            throw NoSuchElementException()
        }
        if (!videoWithChannelsRepository.existsByChannel_OwnerIdAndVideoId(userId, videoId)) {
            throw IllegalAccessException()
        }
        val path = Paths.get(TEMP_VIDEO_SOURCE_BASE_PATH, videoId.toString())
        if (path.notExists()) {
            path.createDirectory()
        }
        return saveFile(
            input = source,
            path = "$path/$chunkIndex.tmp"
        )
    }

    override fun confirm(userId: Long, videoId: Long) {
        val videoEntity = videoRepository
            .findById(videoId)
            .orElseThrow()
            .copy(state = VideoState.UPLOADED)
        if (
            !channelRepository.existsByOwnerIdAndChannelId(
                userId = userId,
                channelId = videoEntity.channelId
            )
        ) {
            throw IllegalAccessException()
        }
        videoRepository.save(videoEntity)
        videoSearchRepository.save(videoEntity.toDocument())
        assembleFile(videoId)
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
    }

    private fun assembleFile(videoId: Long) {
        val tempPath = Path(
            TEMP_VIDEO_SOURCE_BASE_PATH,
            videoId.toString()
        )
        val metaData =
            (tempPath.resolve(Path(videoId.toString())))
                .toFile()
                .inputStream()
                .use {
                    it.readBytes()
                        .decodeToString()
                        .let {
                            Gson().fromJson(it, VideoMetaData::class.java)
                        }
                }
        val extension = metaData.fileName.parseExtension()
        val file = Path(VIDEOS_PLAYABLES_BASE_PATH, "$videoId.$extension").toFile()
        file.outputStream().use { output ->
            tempPath.toFile().listFiles { _, name ->
                name.endsWith(".tmp")
            }
                ?.sortedBy { it.name }
                ?.forEach {
                    it.inputStream().use { input ->
                        val buffer = ByteArray(IO_BUFFER_SIZE)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                        }
                    }
                }
        }
    }

    override fun checkOwner(userId: Long, videoId: Long): Boolean {
        return videoWithChannelsRepository.existsByChannel_OwnerIdAndVideoId(userId, videoId)
    }

    override fun getRecommendations(userId: Long, partIndex: Long, partSize: Int): List<VideoWithChannel> {
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

    override fun delete(userId: Long, videoId: Long) {
        if (!videoWithChannelsRepository.existsByChannel_OwnerIdAndVideoId(userId = userId, videoId = videoId)) {
            throw IllegalAccessException()
        }
        if (!videoRepository.existsById(videoId)) {
            throw NoSuchElementException()
        }
        videoRepository.deleteById(videoId)
        videoSearchRepository.deleteById(videoId)
        findFileByName(File(VIDEOS_PLAYABLES_BASE_PATH), videoId.toString())?.delete()
        findFileByName(File(VIDEOS_COVERS_BASE_PATH), videoId.toString())?.delete()
    }

    override fun getCover(videoId: Long): Resource {
        return FileSystemResource(
            findFileByName(VIDEOS_COVERS_BASE_PATH, videoId.toString())
                .takeUnless { !videoRepository.existsById(videoId) || it?.exists() != true }
                ?: throw NoSuchElementException()
        )
    }

    override fun edit(
        userId: Long,
        video: Video,
        coverAction: EditAction,
        cover: UploadedFile?
    ): Video {
        val videoEntity = videoRepository
            .findById(video.videoId!!)
            .orElseThrow()
            .copy(title = video.title)
        if (!videoWithChannelsRepository.existsByChannel_OwnerIdAndVideoId(userId = userId, videoId = video.videoId)) {
            throw IllegalAccessException()
        }
        val updatedVideoEntity = videoRepository.save(videoEntity)
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
