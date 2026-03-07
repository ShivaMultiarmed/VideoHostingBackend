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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mikhail.shell.video.hosting.dto.camelToSnakeCase
import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.domain.ValidationRules.FILE_NAME_REGEX
import mikhail.shell.video.hosting.domain.ValidationRules.MAX_VIDEO_SIZE
import mikhail.shell.video.hosting.elastic.documents.VideoDocument
import mikhail.shell.video.hosting.elastic.documents.toDocument
import mikhail.shell.video.hosting.elastic.repository.VideoSearchRepository
import mikhail.shell.video.hosting.entities.*
import mikhail.shell.video.hosting.errors.*
import mikhail.shell.video.hosting.os.Executable
import mikhail.shell.video.hosting.repository.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.data.domain.PageRequest
import org.springframework.data.elasticsearch.client.elc.NativeQuery
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.search
import org.springframework.stereotype.Service
import ws.schild.jave.process.ffmpeg.DefaultFFMPEGLocator
import java.awt.image.BufferedImage
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
    private val appPaths: ApplicationPaths,
    private val imageValidator: FileValidator.ImageValidator,
    private val videoValidator: FileValidator.VideoValidator,
    private val videoMetaDataExtractor: VideoMetaDataExtractor
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

        val liking = when {
            !userLikeVideoRepository.existsById(likingId) -> Liking.NONE
            else -> userLikeVideoRepository.findById(likingId).get().liking
        }

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
                    return params.date_time * doc['date_time'].value.millis
                     + params.views * doc['views'].value 
                     + params.likes * doc['likes'].value 
                     + params.dislikes * doc['dislikes'].value;
                """.trimIndent()
            )
            .params(
                mapOf(
                    "date_time" to JsonData.of(recommendationWeights.dateTime),
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
                        it.field("video_id").order(SortOrder.Asc)
                    }.build()
            )
            .let {
                if (cursor != null) {
                    val lastScore = videoRepository.findById(cursor).orElseThrow().let {
                        it.dateTime.toEpochMilli() * recommendationWeights.dateTime + it.views * recommendationWeights.views + it.likes * recommendationWeights.likes + it.dislikes * recommendationWeights.dislikes
                    }
                    it.withSearchAfter(listOf(lastScore, cursor))
                } else it
            }
            .withPageable(PageRequest.of(0, partSize))
            .build()
        val ids = elasticSearchOperations.search<VideoDocument>(nativeQuery).map { it.content.videoId }
        return videoWithChannelsRepository.findAllById(ids).map { it.toDomain() }
    }

    @OptIn(ExperimentalPathApi::class)
    override fun save(
        userId: Long,
        video: VideoCreationModel
    ): PendingVideo {
        val errors = mutableMapOf<String, Error>()
        if (!channelRepository.existsById(video.channelId)) {
            errors["channel_id"] = NumericError.NOT_EXISTS
        }
        if (!channelRepository.existsByOwnerIdAndChannelId(userId, video.channelId)) {
            throw IllegalAccessException()
        }
        val tmpId = UUID.randomUUID()
        val tmpPath = Path(appPaths.TEMP_PATH, tmpId.toString()).createDirectory()
        video.cover?.let {
            val ext = it.name.parseExtension()
            val tmpCoverPath = tmpPath.resolve("cover.$ext")
            runBlocking(Dispatchers.IO) {
                it.content.inputStream().uploadFile(
                    targetFile = tmpCoverPath
                )
                imageValidator.validate(tmpCoverPath.toFile()).onFailure { error ->
                    errors["cover"] = error
                }
            }
        }
        if (errors.isNotEmpty()) {
            tmpPath.deleteRecursively()
            throw ValidationException(errors)
        }
        val pendingVideoEntity = pendingVideosRepository.save(
            PendingVideoEntity(
                tmpId = tmpId,
                channelId = video.channelId,
                title = video.title,
                description = video.description,
                dateTime = Instant.now(),
                fileName = "source." + video.source.fileName!!.parseExtension(),
                mimeType = video.source.mimeType!!,
                size = video.source.size!!
            )
        )
        return PendingVideo(
            tmpId = tmpId,
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
        runBlocking {
            source.uploadFile(
                targetFile = sourcePath
                    .resolve("$start-$end.tmp")
                    .createFile()
            )
        }
    }

    @OptIn(ExperimentalPathApi::class)
    override fun confirm(
        userId: Long,
        tmpId: UUID
    ) {
        val errors = mutableMapOf<String, Error>()
        val pending = pendingVideosRepository.findById(tmpId).orElseThrow()
        if (!channelRepository.existsByOwnerIdAndChannelId(userId, pending.channelId)) {
            throw IllegalAccessException()
        }
        coroutineScope.launch {
            val pendingSourceMetaData = getSourceMetaData(pending.tmpId)
            val tmpPath = Path(appPaths.TEMP_PATH, tmpId.toString()).createDirectories()
            assembleVideoSource(
                tmpPath = tmpPath,
                metaData = pendingSourceMetaData
            )
            val tmpSource = findFileByName(tmpPath, "source")!!
            val tmpSourceMetaData = videoMetaDataExtractor.extract(tmpSource)!!
            if (tmpSourceMetaData.size == null || tmpSourceMetaData.size == 0L) {
                errors["source"] = FileError.EMPTY
            } else if (tmpSourceMetaData.size > MAX_VIDEO_SIZE) {
                errors["source"] = FileError.LARGE
            } else if (
                tmpSourceMetaData.fileName == null
                || !tmpSourceMetaData.fileName.matches(FILE_NAME_REGEX.toRegex())
                || !tmpSourceMetaData.mimeType!!.startsWith("video")
            ) {
                errors["source"] = FileError.NOT_SUPPORTED
            } else if (pendingSourceMetaData != tmpSourceMetaData) {
                errors["source"] = FileError.NOT_VALID
            } else {
                videoValidator.validate(tmpSource).onFailure { error ->
                    errors["source"] = error
                }
            }
            if (errors.isNotEmpty()) {
                pendingVideosRepository.delete(pending)
                tmpPath.deleteRecursively()
                notifyCreatorOnFailure(pending.channelId, errors)
                return@launch
            }
            val videoEntity = videoRepository.save(
                VideoEntity(
                    channelId = pending.channelId,
                    title = pending.title,
                    description = pending.description,
                    dateTime = pending.dateTime
                )
            )
            videoSearchRepository.save(videoEntity.toDocument())
            val videoPath = Path(appPaths.VIDEOS_BASE_PATH, videoEntity.videoId.toString()).createDirectories()
            val sourceJob = launch {
                moveVideoSource(
                    tmpPath = tmpPath,
                    videoPath = videoPath
                )
            }
            val coversJob = findFileByName(tmpPath, "cover")?.let {
                val cover = it.inputStream().toImage() ?: return@let null
                val coverDirectoryPath = videoPath.resolve("cover").createDirectory()
                launch {
                    cover.moveVideoCovers(
                        tmpCoverPath = it.toPath(),
                        coverDirectoryPath = coverDirectoryPath
                    )
                }
            }
            setOfNotNull(sourceJob, coversJob).joinAll()
            pendingVideosRepository.delete(pending)
            tmpPath.deleteRecursively()
            val videoWithChannel = videoWithChannelsRepository.findById(videoEntity.videoId!!).orElseThrow().toDomain()
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
                file.delete()
            }
        }
        tmpPath.resolve("source").deleteExisting()
    }

    private fun moveVideoSource(
        tmpPath: Path,
        videoPath: Path
    ) {
        val tmpSource = findFileByName(tmpPath, "source")!!.toPath()
        val extension = tmpSource.extension
        val sourcePath = videoPath.resolve("source").createDirectory()
        val source = sourcePath.resolve("original.$extension")
        repairVideo(tmpSource.toFile(), source.toFile())
    }

    private fun repairVideo(
        input: File,
        output: File
    ) {
        try {
            val exe = Executable(DefaultFFMPEGLocator().executablePath)
            val fastCommand = arrayOf(
                "-i", input.absolutePath,
                "-c", "copy",
                "-movflags", "faststart",
                "-y",
                output.absolutePath
            )
            var result = exe.execute(fastCommand)
            if (result != 0) {
                val fullScaleCommand = arrayOf(
                    "-i", input.absolutePath,
                    "-c:v", "libx264", "-preset", "ultrafast",
                    "-c:a", "aac", "-movflags", "faststart",
                    "-y", output.absolutePath
                )
                result = exe.execute(fullScaleCommand)
            }
            if (result != 0) {
                throw RuntimeException()
            }
        } catch (e: Exception) {
            input.toPath().moveTo(output.toPath())
            e.printStackTrace()
        }
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
            ).content.map { it.toDomain() }
            .toList()
    }

    private suspend fun BufferedImage.moveVideoCovers(
        tmpCoverPath: Path,
        coverDirectoryPath: Path
    ): Job = coroutineScope {
        launch {
            uploadImage(
                targetFile = coverDirectoryPath.resolve("small.${tmpCoverPath.extension}"),
                targetWidth = 128,
                targetHeight = 72,
                compress = true
            )
        }
        launch {
            uploadImage(
                targetFile = coverDirectoryPath.resolve("medium.${tmpCoverPath.extension}"),
                targetWidth = 320,
                targetHeight = 180,
                compress = true
            )
        }
        launch {
            uploadImage(
                targetFile = coverDirectoryPath.resolve("large.${tmpCoverPath.extension}"),
                targetWidth = 512,
                targetHeight = 288,
                compress = true
            )
        }
    }

    private fun notifyAllOnSuccess(videoWithChannel: VideoWithChannel) {
        val channelId = videoWithChannel.channel.channelId
        val subscribersTopic = "channels.$channelId.subscribers"
        val creatorTopic = "channels.$channelId.uploads"
        val videoWithChannelDto = mapOf(
            "channel_title" to videoWithChannel.channel.title,
            "video_title" to videoWithChannel.video.title,
            "video_id" to videoWithChannel.video.videoId.toString()
        )
        val message = { topic: String ->
            Message.builder()
                .setTopic(topic)
                .putAllData(videoWithChannelDto)
                .build()!!
        }
        fcm.send(message(subscribersTopic))
        fcm.send(message(creatorTopic))
    }

    private fun notifyCreatorOnFailure(
        channelId: Long,
        errors: Map<String, Error>
    ) {
        val topic = "channels.$channelId.uploads"
        val errorsDto = errors
            .map { "${it.key}Error".camelToSnakeCase() to it.value.toString() }
            .toMap()
        val message = Message.builder()
            .setTopic(topic)
            .putAllData(errorsDto)
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
        val errors = mutableMapOf<String, Error>()
        val tmpId = UUID.randomUUID()
        val tmpPath = Path(appPaths.TEMP_PATH, tmpId.toString()).createDirectory()
        if (video.cover is EditingAction.Edit) {
            val ext = video.cover.value.name.parseExtension()
            val tmpCoverPath = tmpPath.resolve("cover.$ext").createFile()
            runBlocking(Dispatchers.IO) {
                video.cover.value.content.inputStream().uploadFile(
                    targetFile = tmpCoverPath
                )
                imageValidator.validate(tmpCoverPath.toFile()).onFailure { error ->
                    errors["cover"] = error
                }
            }
        }
        if (errors.isNotEmpty()) {
            tmpPath.deleteRecursively()
            throw ValidationException(errors)
        }
        val updatedVideoEntity = videoRepository.save(
            videoEntity.copy(
                title = video.title,
                description = video.description
            )
        )
        videoSearchRepository.save(updatedVideoEntity.toDocument())
        val videoPath = Path(appPaths.VIDEOS_BASE_PATH, video.videoId.toString()).createDirectories()
        val coverPath = videoPath.resolve("cover")
        if (video.cover is EditingAction.Remove) {
            if (coverPath.exists()) {
                coverPath.deleteRecursively()
            }
        } else if (video.cover is EditingAction.Edit) {
            if (coverPath.notExists()) {
                coverPath.createDirectory()
            } else {
                coverPath.listDirectoryEntries().forEach { it.deleteExisting() }
            }
            runBlocking {
                findFileByName(tmpPath, "cover")?.let {
                    val cover = it.inputStream().toImage() ?: return@let
                    cover.moveVideoCovers(
                        tmpCoverPath = it.toPath(),
                        coverDirectoryPath = coverPath
                    )
                }
            }
        }
        tmpPath.deleteRecursively()
        return updatedVideoEntity.toDomain()
    }

    @PreDestroy
    fun preDestroy() {
        coroutineScope.cancel()
    }
}
