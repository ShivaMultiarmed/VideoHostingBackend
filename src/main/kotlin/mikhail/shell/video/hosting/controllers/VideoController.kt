package mikhail.shell.video.hosting.controllers

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.domain.ValidationRules.FILE_NAME_REGEX
import mikhail.shell.video.hosting.domain.ValidationRules.MAX_VIDEO_SIZE
import mikhail.shell.video.hosting.dto.*
import mikhail.shell.video.hosting.errors.FileError
import mikhail.shell.video.hosting.service.ChannelService
import mikhail.shell.video.hosting.service.VideoService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.*
import kotlin.io.path.Path

@RestController
@RequestMapping("/api/v2/videos")
class VideoController @Autowired constructor(
    private val videoService: VideoService,
    private val channelService: ChannelService,
    private val appPaths: ApplicationPathsInitializer
) {
    @Value("\${video-hosting.server.base-url}")
    private lateinit var BASE_URL: String

    @GetMapping("/{video_id}")
    fun get(@PathVariable("video_id") @LongId videoId: Long?): VideoDto {
        return videoService.get(videoId!!).toDto()
    }

    @GetMapping("/{video_id}/details")
    fun getVideoDetails(
        @PathVariable("video_id") @LongId videoId: Long?,
        @AuthenticationPrincipal userId: Long
    ): VideoDetailsDto {
        val videoDto = videoService.get(videoId = videoId!!, userId = userId).toDto()
        val channelDto = channelService.getForUser(channelId = videoDto.channelId, userId = userId).toDto()
        return VideoDetailsDto(
            video = videoDto,
            channel = channelDto
        )
    }

    @PatchMapping("/{video_id}/rate")
    fun rateVideo(
        @PathVariable("video_id") @LongId videoId: Long?,
        @RequestParam("liking") @ValidEnum(Liking::class) liking: String?,
        @AuthenticationPrincipal userId: Long
    ): VideoWithUserDto {
        return videoService.rate(videoId = videoId!!, userId = userId, liking = Liking.valueOf(liking!!.uppercase())).toDto()
    }

    @GetMapping("/{video_id}/source")
    fun playVideo(
        @PathVariable("video_id") @LongId videoId: Long?,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        val sourceDirectory = Path(appPaths.VIDEOS_BASE_PATH, videoId.toString(), "source")
        val file = findFileByName(sourceDirectory, "original")
        if (file?.exists() != true || !videoService.checkExistence(videoId!!)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
        val rangeHeader = request.getHeader(HttpHeaders.RANGE)

        if (rangeHeader == null) {
            response.contentType = "video/${file.name.parseExtension()}"
            response.setContentLengthLong(file.length())
            file.inputStream().use {
                it.copyTo(response.outputStream)
            }
            return ResponseEntity.ok().build()
        }

        val range = rangeHeader.replace("bytes=", "").split("-")
        val start = range[0].toLong()
        val end = if (range.size > 1 && range[1].isNotEmpty()) {
            range[1].toLong()
        } else {
            file.length() - 1
        }

        if (start > end || end >= file.length()) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                .header(HttpHeaders.CONTENT_RANGE, "bytes */${file.length()}")
                .build()
        }

        val contentLength = end - start + 1
        response.status = HttpStatus.PARTIAL_CONTENT.value()
        response.contentType = "video/${file.name.parseExtension()}"
        response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes")
        response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes $start-$end/${file.length()}")
        response.setContentLengthLong(contentLength)

        RandomAccessFile(file, "r").use {
            it.seek(start)
            val buffer = ByteArray(1024 * 8)
            var bytesRead: Int
            var bytesLeft = contentLength
            while (bytesLeft > 0) {
                bytesRead = it.read(buffer, 0, minOf(buffer.size.toLong(), bytesLeft).toInt())
                if (bytesRead == -1) {
                    break
                }
                response.outputStream.write(buffer, 0, bytesRead)
                response.outputStream.flush()
                bytesLeft -= bytesRead
            }
        }

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).build()
    }

    @GetMapping("/channel/{channel_id}")
    fun provideVideosFromChannel(
        @PathVariable("channel_id") @LongId channelId: Long?,
        @RequestParam("part_size") @PartSize partSize: Int = 10,
        @RequestParam("part_index") @PartIndex partIndex: Long = 0
    ): List<VideoDto> {
        return videoService.getByChannelId(
            channelId = channelId!!,
            partSize = partSize,
            partIndex = partIndex
        ).map {
            it.toDto()
        }
    }

    @GetMapping("/{video_id}/cover")
    fun provideVideoCover(
        @PathVariable("video_id") @LongId videoId: Long?,
        @RequestParam("size") @ValidEnum(ImageSize::class) size: String?
    ): ResponseEntity<Resource> {
        val image = videoService.getCover(videoId!!, ImageSize.valueOf(size!!.uppercase()))
        return ResponseEntity.status(HttpStatus.OK)
            .contentType(MediaType.parseMediaType("image/${image.file.extension}"))
            .body(image)
    }

    @GetMapping("/search")
    fun search(
        @RequestParam("query") @Title query: String?,
        @RequestParam("cursor") @LongIdNullable cursor: Long?,
        @RequestParam("part_size") @PartSize partSize: Int = 10
    ): List<VideoWithChannelDto> {
        return videoService.getByQuery(
            query = query!!,
            partSize = partSize,
            cursor = cursor
        ).map {
            VideoWithChannelDto(
                video = it.video.toDto(),
                channel = it.channel.toDto()
            )
        }
    }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        @RequestPart("video") @Valid video: VideoCreationRequest,
        @RequestPart("source") source: VideoMetaData,
        @RequestPart("cover") @Image cover: MultipartFile?,
        @AuthenticationPrincipal userId: Long
    ): ResponseEntity<*> {
        if (source.size == 0L || source.size == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("source" to FileError.EMPTY))
        } else if (source.size > MAX_VIDEO_SIZE) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("source" to FileError.LARGE))
        }
        if (source.fileName == null || !source.fileName.matches(FILE_NAME_REGEX.toRegex())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("source" to FileError.NAME_NOT_VALID))
        }
        // TODO: check video type here and after assembling
        //val detectedMimeType = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(source.fileName)?: ""
        if (!source.mimeType!!.startsWith("video")) { // || detectedMimeType != source.mimeType) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("source" to FileError.NOT_SUPPORTED))
        }
        return videoService.save(
            userId = userId,
            video = VideoCreationModel(
                channelId = video.channelId!!,
                title = video.title!!,
                description = video.description,
                cover = cover?.toUploadedFile(),
                source = source
            )
        ).let {
            ResponseEntity.status(HttpStatus.OK).body(PendingVideoDto(tmpId = it.tmpId))
        }
    }

    @PostMapping("/{tmp_id}/source", consumes = ["application/octet-stream"])
    fun uploadSource(
        @PathVariable("tmp_id") @ValidUUID tmpId: String?,
        @RequestHeader("Content-Range") contentRange: String,
        input: InputStream,
        @AuthenticationPrincipal userId: Long
    ) {
        val groups = """(\d+)-(\d+)/(\d+)$""".toRegex()
            .find(contentRange)
            ?.groupValues
        if (groups == null || groups.size != 4) { // including full match
            throw IllegalArgumentException()
        }
        val (start, end, total) = groups.slice(1 .. 3).map { it.toLong() }
        if (start > end || total <= 0 || start < 0 || end - start + 1 > BUFFER_SIZE) {
            throw IllegalArgumentException()
        }
        videoService.saveVideoSource(
            userId = userId,
            tmpId = UUID.fromString(tmpId!!),
            start = start,
            end = end + 1,
            source = input
        )
    }

    @PostMapping("/{tmp_id}/confirmation")
    fun confirmUpload(
        @PathVariable("tmp_id") @ValidUUID tmpId: String?,
        @AuthenticationPrincipal userId: Long
    ) {
        videoService.confirm(
            userId = userId,
            tmpId = UUID.fromString(tmpId!!)
        )
    }

    @PatchMapping("/{video_id}/views")
    fun incrementViews(@PathVariable("video_id") @LongId videoId: Long?): VideoDto {
        return videoService.incrementViews(videoId!!).toDto()
    }

    @PutMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun editVideo(
        @RequestPart("video") @Valid video: VideoEditingRequest,
        @RequestPart("cover") @Image cover: MultipartFile?,
        @AuthenticationPrincipal userId: Long
    ): VideoDto {
        return videoService.edit(
            userId = userId,
            video = VideoEditingModel(
                videoId = video.videoId!!,
                title = video.title!!,
                description = video.description,
                cover = when (EditAction.valueOf(video.coverAction!!.uppercase())) {
                    EditAction.KEEP -> EditingAction.Keep
                    EditAction.REMOVE -> EditingAction.Remove
                    EditAction.UPDATE -> EditingAction.Edit(cover!!.toUploadedFile())
                }
            ),
        ).toDto()
    }

    @DeleteMapping("/{video_id}")
    fun delete(
        @PathVariable("video_id") @LongId videoId: Long?,
        @AuthenticationPrincipal userId: Long
    ) {
        videoService.delete(
            userId = userId,
            videoId = videoId!!
        )
    }

    @GetMapping("/recommendations")
    fun getRecommendations(
        @RequestParam("part_index") @PartIndex partIndex: Long = 0,
        @RequestParam("part_size") @PartSize partSize: Int = 10,
        @AuthenticationPrincipal userId: Long
    ): List<VideoWithChannelDto> {
        return videoService.getRecommendations(
            userId = userId,
            partIndex = partIndex,
            partSize = partSize
        ).map {
            VideoWithChannelDto(
                video = it.video.toDto(),
                channel = it.channel.toDto()
            )
        }
    }

    @PostMapping("/search/sync")
    fun syncSearchIndex() {
        videoService.sync()
    }

    private companion object {
        const val BUFFER_SIZE = 10 * 1024 * 1024
    }
}

data class VideoCreationRequest(
    @field:Title
    val title: String?,
    @field:LongId
    val channelId: Long?,
    @field:Description
    val description: String?
)

data class VideoMetaData(
    val fileName: String?,
    val mimeType: String?,
    val size: Long?
)

data class VideoEditingRequest(
    @field:LongId
    val videoId: Long?,
    @field:Title
    val title: String?,
    @field:ValidEnum(EditAction::class)
    val coverAction: String?,
    @field:Description
    val description: String?
)
