package mikhail.shell.video.hosting.service

import co.elastic.clients.elasticsearch._types.Script
import co.elastic.clients.elasticsearch._types.ScriptSort
import co.elastic.clients.elasticsearch._types.ScriptSortType
import co.elastic.clients.elasticsearch._types.SortOptions
import co.elastic.clients.elasticsearch._types.SortOrder
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders
import co.elastic.clients.json.JsonData
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import jakarta.transaction.Transactional
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mikhail.shell.video.hosting.controllers.VideoMetaData
import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.elastic.documents.VideoDocument
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
import java.io.InputStream
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import javax.annotation.PreDestroy
import kotlin.io.extension
import kotlin.io.path.*

@Service
class VideoServiceWithDB @Autowired constructor(
    private val recommendationWeights: RecommendationWeights,
    private val pendingVideosRepository: PendingVideosRepository,
    private val videoRepository: VideoRepository,
    private val videoWithChannelsRepository: VideoWithChannelsRepository,
    private val videoSearchRepository: VideoSearchRepository,
    private val elasticSearchOperations: ElasticsearchOperations,
    private val userRepository: UserRepository,
    private val userLikeVideoRepository: UserLikeVideoRepository,
    private val channelRepository: ChannelRepository,
    private val fcm: FirebaseMessaging,
    private val appPaths: ApplicationPathsInitializer
) : VideoService {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun get(videoId: Long): Video {
        return videoRepository.findById(videoId).orElseThrow().toDomain()
    }

    override fun get(
        videoId: Long,
        userId: Long
    ): VideoWithUser {
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

    override fun checkLiking(
        videoId: Long,
        userId: Long
    ): Liking {
        val id = VideoLikingId(userId, videoId)
        return userLikeVideoRepository.findById(id).orElse(null)?.liking ?: Liking.NONE
    }

    @Transactional
    override fun rate(
        videoId: Long,
        userId: Long,
        liking: Liking
    ): VideoWithUser {
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
        partIndex: Long
    ): List<Video> {
        if (!channelRepository.existsById(channelId)) {
            throw NoSuchElementException()
        }
        return videoRepository.findByChannelIdOrderByDateTimeDesc(
            channelId = channelId,
            pageable = PageRequest.of(
                partIndex.toInt(),
                partSize
            )
        ).map { it.toDomain() }
    }

    override fun getByQuery(
        query: String,
        partSize: Int,
        cursor: Long?
    ): List<VideoWithChannel> {
        val sortingScript = Script.Builder()
            .lang("painless")
            .source(
                """
                    return params.views * doc['views'].value 
                     + params.likes * doc['likes'].value 
                     + params.dislikes * doc['dislikes'].value;
                """.trimIndent()
            )
            .params(
                mapOf(
                    //"dateTime" to JsonData.of(recommendationWeights.dateTime), // TODO: take the dateTime into account
                    "views" to JsonData.of(recommendationWeights.views),
                    "likes" to JsonData.of(recommendationWeights.likes),
                    "dislikes" to JsonData.of(recommendationWeights.dislikes)
                )
            )
            .build()
        val sortOptions = ScriptSort.Builder()
            .script(sortingScript)
            .order(SortOrder.Desc)
            .type(ScriptSortType.Number)
            .build()
            ._toSortOptions()
        val queryBuilder = QueryBuilders.bool {
            it.should {
                it.match {
                    it.field("title").query(query)
                }
            }
            it.should {
                it.match {
                    it.field("description").query(query)
                }
            }
        }
        val nativeQuery = NativeQuery.builder()
            .withQuery(queryBuilder)
            .withSort(sortOptions)
            .withSort(
                SortOptions.Builder()
                    .field {
                        it.field("videoId").order(SortOrder.Asc)
                    }.build()
            )
            .let {
                if (cursor != null) {
                    val lastScore = videoRepository.findById(cursor).orElseThrow().let {
                        it.views * recommendationWeights.views + it.likes * recommendationWeights.likes + it.dislikes * recommendationWeights.dislikes
                    }
                    it.withSearchAfter(listOf(lastScore, cursor))
                } else it
            }
            .withPageable(PageRequest.of(0, partSize))
            .build()
        val ids = elasticSearchOperations.search<VideoDocument>(nativeQuery).map { it.content.videoId }
        return videoWithChannelsRepository.findAllById(ids).map { it.toDomain() }
    }

    override fun save(
        userId: Long,
        video: VideoCreationModel
    ): PendingVideo {
        val errors = mutableMapOf<String, Error>()
        if (!channelRepository.existsById(video.channelId)) {
            errors["channel_id"] = NumericError.NOT_EXISTS
        }
        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }
        if (!channelRepository.existsByOwnerIdAndChannelId(userId, video.channelId)) {
            throw IllegalAccessException()
        }
        val pendingVideoEntity = pendingVideosRepository.save(
            PendingVideoEntity(
                channelId = video.channelId,
                title = video.title,
                description = video.description,
                dateTime = Instant.now(),
                fileName = video.source.fileName!!,
                mimeType = video.source.mimeType!!,
                size = video.source.size!!
            )
        )
        val tmpPath = Path(appPaths.TEMP_PATH, pendingVideoEntity.tmpId.toString()).createDirectory()
        video.cover?.let {
            val ext = it.fileName.parseExtension()
            val coverPath = tmpPath.resolve("cover.$ext").createFile()
            runBlocking {
                uploadImage(
                    uploadedFile = it,
                    targetFile = coverPath.toString()
                )
            }
        }
        return PendingVideo(
            tmpId = pendingVideoEntity.tmpId,
            channelId = pendingVideoEntity.channelId
        )
    }

    override fun saveVideoSource(
        userId: Long,
        tmpId: UUID,
        start: Long,
        end: Long,
        source: InputStream
    ) {
        val channelId = pendingVideosRepository
            .findById(tmpId)
            .orElseThrow()
            .channelId
        if (!channelRepository.existsByOwnerIdAndChannelId(userId, channelId)) {
            throw IllegalAccessException()
        }
        val path = Path(appPaths.TEMP_PATH, tmpId.toString()).createDirectories()
        val sourcePath = path.resolve("source").createDirectories()
        writeToFile(
            input = source,
            path = sourcePath
                .resolve("$start-$end.tmp")
                .createFile()
                .toString()
        )
    }

    @OptIn(ExperimentalPathApi::class)
    override fun confirm(
        userId: Long,
        tmpId: UUID
    ) {
        val pending = pendingVideosRepository.findById(tmpId).orElseThrow()
        if (!channelRepository.existsByOwnerIdAndChannelId(userId, pending.channelId)) {
            throw IllegalAccessException()
        }
        coroutineScope.launch {
            val sourceMetaData = getSourceMetaData(pending.tmpId)
            val tmpPath = Path(appPaths.TEMP_PATH, tmpId.toString())
            assembleVideoSource(
                tmpPath = tmpPath,
                metaData = sourceMetaData
            )
            val videoEntity = videoRepository.save(
                VideoEntity(
                    channelId = pending.channelId,
                    title = pending.title,
                    description = pending.description,
                    dateTime = pending.dateTime
                )
            )
            videoSearchRepository.save(videoEntity.toDocument())
            val sourceJob = launch {
                moveVideoSource(
                    tmpPath = tmpPath,
                    metaData = sourceMetaData,
                    videoId = videoEntity.videoId!!
                )
            }
            val coversJob = launch {
                moveVideoCovers(
                    tmpPath = tmpPath,
                    videoId = videoEntity.videoId!!
                )
            }
            val videoWithChannel = videoWithChannelsRepository.findById(videoEntity.videoId!!).orElseThrow().toDomain()
            pendingVideosRepository.delete(pending)
            joinAll(sourceJob, coversJob)
            tmpPath.deleteRecursively()
            notifyAllOnSuccess(videoWithChannel)
        }
    }

    private fun getSourceMetaData(tmpId: UUID): VideoMetaData {
        return pendingVideosRepository.findById(tmpId).orElseThrow().let {
            VideoMetaData(
                fileName = it.fileName,
                mimeType = it.mimeType,
                size = it.size
            )
        }
    }

    private fun assembleVideoSource(
        tmpPath: Path,
        metaData: VideoMetaData
    ) {
        val extension = metaData
            .fileName!!
            .parseExtension()
        val tmpSourceChunks = tmpPath
            .resolve("source")
            .toFile()
            .listFiles { it: File -> it.extension == "tmp" }!!
            .sortedBy { it.nameWithoutExtension.substringBefore("-").toLong() }
        val tmpSource = tmpPath
            .resolve("source.$extension")
            .createFile()
            .toFile()
        tmpSource.outputStream().use { fos ->
            tmpSourceChunks.forEach { file: File ->
                file.inputStream().use { fis ->
                    fis.copyTo(fos)
                }
            }
        }
    }

    private fun moveVideoSource(
        tmpPath: Path,
        metaData: VideoMetaData,
        videoId: Long
    ) {
        val extension = metaData.fileName!!.parseExtension()
        val tmpSource = tmpPath.resolve("source.$extension")
        val path = Path(appPaths.VIDEOS_BASE_PATH, videoId.toString()).createDirectory()
        val sourcePath = path.resolve("source").createDirectory()
        val source = sourcePath.resolve("original.$extension")
        tmpSource.moveTo(source)
    }

    override fun getRecommendations(
        userId: Long,
        partIndex: Long,
        partSize: Int,
    ): List<VideoWithChannel> {
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

    private suspend fun moveVideoCovers(
        tmpPath: Path,
        videoId: Long
    ) = coroutineScope {
        val tmpCover = findFileByName(tmpPath, "cover")
        tmpCover?.let {
            val coverPath = Path(appPaths.VIDEOS_BASE_PATH, videoId.toString(), "cover").createDirectories()
            val smallImage = launch {
                uploadImage(
                    uploadedFile = it,
                    targetFile = "$coverPath/small.${it.extension}",
                    width = 128,
                    height = 72,
                    compress = true
                )
            }
            val mediumImage = launch {
                uploadImage(
                    uploadedFile = it,
                    targetFile = "$coverPath/medium.${it.extension}",
                    width = 320,
                    height = 180,
                    compress = true
                )
            }
            val largeImage = launch {
                uploadImage(
                    uploadedFile = it,
                    targetFile = "$coverPath/large.${it.extension}",
                    width = 512,
                    height = 288,
                    compress = true
                )
            }
            setOf(smallImage, mediumImage, largeImage).joinAll()
        }
    }

    private fun notifyAllOnSuccess(videoWithChannel: VideoWithChannel) {
        val channelId = videoWithChannel.channel.channelId
        val subscribersTopic = "channels.$channelId.subscribers"
        val creatorTopic = "channels.$channelId.uploads"
        val data = mapOf(
            "channel_title" to videoWithChannel.channel.title,
            "video_title" to videoWithChannel.video.title,
            "video_id" to videoWithChannel.video.videoId.toString()
        )
        val message = { topic: String ->
            Message.builder()
                .setTopic(topic)
                .putAllData(data)
                .build()!!
        }
        fcm.send(message(subscribersTopic))
        fcm.send(message(creatorTopic))
    }

    private fun notifyCreatorOnFailure(
        channelId: Long,
        error: Error = UnexpectedError
    ) {
        val topic = "channels.$channelId.uploads"
        val data = mapOf("source_error" to error)
            .map { it.key to it.value.toString() }
            .toMap()
        val message = Message.builder()
            .setTopic(topic)
            .putAllData(data)
            .build()
        fcm.send(message)
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

    private fun writeToFile(
        input: InputStream,
        path: String
    ) {
        File(path).outputStream().use { outStream ->
            input.copyTo(outStream, BUFFER_SIZE)
        }
    }

    @Transactional
    override fun incrementViews(videoId: Long): Video {
        val video = videoRepository.findById(videoId).orElseThrow()
        videoRepository.save(video.copy(views = video.views + 1))
        videoSearchRepository.save(video.toDocument().copy(views = video.views + 1))
        return videoRepository.findById(videoId).get().toDomain()
    }

    @OptIn(ExperimentalPathApi::class)
    override fun remove(
        userId: Long,
        videoId: Long
    ) {
        if (!videoRepository.existsById(videoId)) {
            throw NoSuchElementException()
        }
        if (!videoWithChannelsRepository.existsByChannel_OwnerIdAndVideoId(userId = userId, videoId = videoId)) {
            throw IllegalAccessException()
        }
        videoRepository.deleteById(videoId)
        videoSearchRepository.deleteById(videoId)
        Path(appPaths.VIDEOS_BASE_PATH, videoId.toString()).deleteRecursively()
    }

    override fun getCover(
        videoId: Long,
        size: ImageSize
    ): Resource {
        val fileFolder = Path(appPaths.VIDEOS_BASE_PATH, videoId.toString(), "cover")
        val file = findFileByName(fileFolder, size.name.lowercase())
        if (!videoRepository.existsById(videoId) || file == null) {
            throw NoSuchElementException()
        }
        return FileSystemResource(file)
    }

    @OptIn(ExperimentalPathApi::class)
    override fun edit(
        userId: Long,
        video: VideoEditingModel,
    ): Video {
        val videoEntity = videoRepository.findById(video.videoId).orElseThrow()
        if (!videoWithChannelsRepository.existsByChannel_OwnerIdAndVideoId(userId, video.videoId)) {
            throw IllegalAccessException()
        }
        val updatedVideoEntity = videoRepository.save(
            videoEntity.copy(
                title = video.title,
                description = video.description
            )
        )
        videoSearchRepository.save(updatedVideoEntity.toDocument())
        val tmpId = UUID.randomUUID()
        val tmpPath = Path(appPaths.TEMP_PATH, tmpId.toString()).createDirectory()
        if (video.cover is EditingAction.Edit) {
            val ext = video.cover.value.fileName.parseExtension()
            val tmpCoverPath = tmpPath.resolve("cover.$ext").createFile()
            runBlocking {
                uploadImage(
                    uploadedFile = video.cover.value,
                    targetFile = tmpCoverPath.toString()
                )
            }
        }
        val videoPath = Path(appPaths.VIDEOS_BASE_PATH, video.videoId.toString())
        val coverPath = videoPath.resolve("cover")
        if (video.cover is EditingAction.Remove) {
            if (coverPath.exists()) {
                coverPath.deleteRecursively()
            }
        } else if (video.cover is EditingAction.Edit) {
            if (coverPath.notExists()) {
                coverPath.createDirectory()
            } else {
                coverPath.listDirectoryEntries().forEach { it.deleteIfExists() }
            }
            runBlocking {
                moveVideoCovers(
                    tmpPath = tmpPath,
                    videoId = videoEntity.videoId!!
                )
            }
        }
        tmpPath.deleteRecursively()
        return updatedVideoEntity.toDomain()
    }

    @PreDestroy
    fun preDestroy() {
        coroutineScope.cancel()
    }

    private companion object {
        const val BUFFER_SIZE = 10 * 1024 * 1024
    }
}
