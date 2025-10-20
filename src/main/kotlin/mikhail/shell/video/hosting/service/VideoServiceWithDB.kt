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
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Paths
import java.time.Instant
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.notExists
import kotlin.io.path.outputStream

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
    private val appPaths: ApplicationPathsInitializer,
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
        partIndex: Long,
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

//    override fun getByQuery(
//        query: String,
//        partSize: Int,
//        cursor: Long?,
//    ): List<VideoWithChannel> {
//        val (viewWeight, likeWeight, dislikeWeight) = listOf(0.1, 0.3, -0.3)
//        val elasticQuery = QueryBuilders.multiMatch {
//            it.query(query)
//            it.fields("title", "description")
//        }
//        val scoreFunction: (String, Double) -> FunctionScore = { field, weight ->
//            FunctionScore.Builder()
//                .fieldValueFactor(
//                    FieldValueFactorScoreFunction.Builder()
//                        .field(field)
//                        .factor(weight)
//                        .build()
//                ).build()
//        }
//        val scoredQuery = FunctionScoreQuery.of {
//            it.query(elasticQuery)
//            it.functions(
//                mutableListOf(
//                    scoreFunction("views", viewWeight),
//                    scoreFunction("likes", likeWeight),
//                    scoreFunction("dislikes", dislikeWeight)
//                )
//            )
//        }.query()!!
//        val nativeSearchQuery: Query = NativeQuery.builder()
//            .withQuery(scoredQuery)
//            .withSort(
//                SortOptions.Builder()
//                    .score(
//                        SortOptionsBuilders.score()
//                            .order(SortOrder.Desc)
//                            .build()
//                    ).build()
//            ).withSort(
//                SortOptions.Builder()
//                    .field(
//                        SortOptionsBuilders.field {
//                            it.field("videoId")
//                            it.order(SortOrder.Asc)
//                        }.field()
//                    ).build()
//            ).let {
//                if (cursor != null) {
//                    val lastScore = videoRepository.findById(cursor).orElseThrow().let {
//                        it.views * viewWeight + it.likes * likeWeight + it.dislikes
//                    }
//                    it.withSearchAfter(
//                        mutableListOf<Any>(cursor)
//                    )
//                } else it
//            }.withPageable(
//                PageRequest.of(0, partSize)
//            ).build()
//        val videoIds = elasticSearchOperations.search<VideoDocument>(nativeSearchQuery).map { it.content.videoId }.toList()
//        return videoWithChannelsRepository.findAllById(videoIds).map { it.toDomain() }
//    }

    override fun getByQuery(
        query: String,
        partSize: Int,
        cursor: Long?,
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
                    //"dateTime" to JsonData.of(recommendationWeights.dateTime),
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
        video: VideoCreationModel,
    ): Long {
        if (!channelRepository.existsByOwnerIdAndChannelId(userId, video.channelId)) {
            throw IllegalAccessException()
        }
        val pendingVideoEntity = pendingVideosRepository.save(
            PendingVideoEntity(
                channelId = video.channelId,
                title = video.title,
                dateTime = Instant.now(),
                fileName = video.source.fileName!!,
                mimeType = video.source.mimeType!!,
                size = video.source.size!!
            )
        )
        val tmpPath = Path(appPaths.TEMP_VIDEOS_BASE_PATH, pendingVideoEntity.uploadId!!.toString()).createDirectory()
        video.cover?.let {
            if (tmpPath.notExists()) {
                tmpPath.createDirectory()
            }
            val coverPath = tmpPath.resolve("cover")
            // TODO adjust image sizes for each case
            uploadImage(
                uploadedFile = it,
                targetFile = "$coverPath/large.png",
                width = 1800,
                height = 200
            )
            uploadImage(
                uploadedFile = it,
                targetFile = "$coverPath/medium.png",
                width = 1000,
                height = 120
            )
            uploadImage(
                uploadedFile = it,
                targetFile = "$coverPath/small.png",
                width = 350,
                height = 60
            )
        }
        return pendingVideoEntity.uploadId!!
    }

    override fun saveVideoSource(
        userId: Long,
        uploadId: Long,
        start: Long,
        end: Long,
        source: InputStream,
    ): Boolean {
        val channelId = pendingVideosRepository.findById(uploadId).orElseThrow().channelId
        if (!channelRepository.existsByOwnerIdAndChannelId(userId, channelId)) {
            throw IllegalAccessException()
        }
        val path = Path(appPaths.TEMP_VIDEOS_BASE_PATH, uploadId.toString()).createDirectory()
        return saveFile(
            input = source,
            path = "$path/$uploadId/source/$start-$end.tmp"
        )
    }

    override fun confirm(
        userId: Long,
        uploadId: Long,
    ): Video {
        val pending = pendingVideosRepository.findById(uploadId).orElseThrow()
        if (!channelRepository.existsByOwnerIdAndChannelId(userId, pending.channelId)) {
            throw IllegalAccessException()
        }
        val videoEntity = videoRepository.save(
            VideoEntity(
                channelId = pending.channelId,
                title = pending.title,
                dateTime = pending.dateTime
            )
        )
        videoSearchRepository.save(videoEntity.toDocument())
        assembleVideoSource(videoEntity.videoId!!, pending.uploadId!!)
        val videoWithChannel = videoWithChannelsRepository.findById(videoEntity.videoId!!).get()
        val message = Message.builder()
            .setTopic("$CHANNELS_TOPICS_PREFIX.${videoWithChannel.channelId}")
            .putAllData(
                mapOf(
                    "channel_title" to videoWithChannel.channel.title,
                    "video_title" to videoWithChannel.title,
                    "video_id" to videoWithChannel.videoId.toString()
                )
            ).build()
        fcm.send(message)
        return videoEntity.toDomain()
    }

    private fun assembleVideoSource(videoId: Long, uploadId: Long) {
        val tmpPath = Path(appPaths.TEMP_VIDEOS_BASE_PATH, uploadId.toString(), "source")
        val metaData = pendingVideosRepository.findById(uploadId).orElseThrow().let {
            VideoMetaData(
                fileName = it.fileName,
                mimeType = it.mimeType,
                size = it.size
            )
        }
        val extension = metaData.fileName!!.parseExtension()
        val file = Path(appPaths.VIDEOS_BASE_PATH, videoId.toString(), "source", "original.$extension")
        file.outputStream().use { output ->
            tmpPath
                .toFile()
                .listFiles { _, name ->
                    name.endsWith(".tmp")
                }?.sortedBy {
                    it.name
                }?.forEach {
                    it.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
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
        findFileByName(File(appPaths.VIDEOS_SOURCES_BASE_PATH), videoId.toString())?.delete()
        findFileByName(File(appPaths.VIDEOS_COVERS_BASE_PATH), videoId.toString())?.delete()
    }

    override fun getCover(videoId: Long): Resource {
        return FileSystemResource(
            findFileByName(appPaths.VIDEOS_COVERS_BASE_PATH, videoId.toString())
                .takeUnless { !videoRepository.existsById(videoId) || it?.exists() != true }
                ?: throw NoSuchElementException()
        )
    }

    override fun edit(
        userId: Long,
        video: Video,
        coverAction: EditAction,
        cover: UploadedFile?,
    ): Video {
        val videoEntity = videoRepository
            .findById(video.videoId)
            .orElseThrow()
            .copy(title = video.title)
        if (!videoWithChannelsRepository.existsByChannel_OwnerIdAndVideoId(userId = userId, videoId = video.videoId)) {
            throw IllegalAccessException()
        }
        val updatedVideoEntity = videoRepository.save(videoEntity)
        videoSearchRepository.save(updatedVideoEntity.toDocument())
        if (coverAction != EditAction.KEEP) {
            findFileByName(Paths.get(appPaths.VIDEOS_COVERS_BASE_PATH).toFile(), video.videoId.toString())?.delete()
        }
        if (cover != null) {
            uploadImage(
                uploadedFile = cover,
                targetFile = "${appPaths.VIDEOS_COVERS_BASE_PATH}/${updatedVideoEntity.videoId}.jpg",
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
