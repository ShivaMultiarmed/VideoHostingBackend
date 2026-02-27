package mikhail.shell.video.hosting.controllers

import jakarta.validation.Valid
import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.domain.ValidationRules.FILE_NAME_REGEX
import mikhail.shell.video.hosting.domain.ValidationRules.MAX_VIDEO_SIZE
import mikhail.shell.video.hosting.dto.*
import mikhail.shell.video.hosting.errors.Error
import mikhail.shell.video.hosting.errors.FileError
import mikhail.shell.video.hosting.errors.ValidationException
import mikhail.shell.video.hosting.service.ChannelService
import mikhail.shell.video.hosting.service.VideoService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.core.io.support.ResourceRegion
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.MediaTypeFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import java.util.*
import kotlin.io.path.Path
import kotlin.math.min

@RestController
@RequestMapping("/api/v2/videos")
class VideoController @Autowired constructor(
    private val videoService: VideoService,
    private val channelService: ChannelService,
    private val appPaths: ApplicationPaths
) {
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
        @RequestParam("liking") liking: Liking,
        @AuthenticationPrincipal userId: Long
    ): VideoWithUserDto {
        return videoService.rate(
            videoId = videoId!!,
            userId = userId,
            liking = liking
        ).toDto()
    }

    @GetMapping("/{video_id}/source")
    fun provideVideoSource(
        @PathVariable("video_id") @LongId videoId: Long?,
        @RequestHeader headers: HttpHeaders
    ): ResponseEntity<ResourceRegion> {
        val path = Path(appPaths.VIDEOS_BASE_PATH, videoId.toString(), "source")
        val file = findFileByName(path, "original")
        if (!videoService.checkExistence(videoId!!) || file == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
        val source = FileSystemResource(file)
        val contentLength = source.contentLength()
        val range = headers.range.firstOrNull()
        val chunkSize = 10 * 1024 * 1024L
        val region = if (range == null) {
            ResourceRegion(source, 0, min(chunkSize, contentLength))
        } else {
            val start = range.getRangeStart(contentLength)
            val end = range.getRangeEnd(contentLength)
            val rangeSize = end - start + 1
            ResourceRegion(source, start, min(chunkSize, rangeSize))
        }
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
            .contentType(MediaTypeFactory.getMediaType(source).orElse(MediaType.APPLICATION_OCTET_STREAM))
            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
            .body(region)
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
        ).map { it.toDto() }
    }

    @GetMapping("/{video_id}/cover")
    fun provideVideoCover(
        @PathVariable("video_id") @LongId videoId: Long?,
        @RequestParam("size") size: ImageSize
    ): ResponseEntity<Resource> {
        val image = videoService.getCover(videoId!!, size)
        return ResponseEntity.status(HttpStatus.OK)
            .contentType(MediaType.parseMediaType("image/${image.file.extension}"))
            .body(image)
    }

    @GetMapping("/search")
    fun search(
        @RequestParam("query") @Title query: String?,
        @RequestParam("cursor") @LongIdNullable cursor: Long? = null,
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
        @RequestPart("source") source: VideoMetaDataDto,
        @RequestPart("cover") @Image cover: MultipartFile?,
        @AuthenticationPrincipal userId: Long
    ): ResponseEntity<PendingVideoDto> {
        val errors = mutableMapOf<String, Error>()
        if (source.size == 0L || source.size == null) {
            errors["source"] = FileError.EMPTY
        } else if (source.size > MAX_VIDEO_SIZE) {
            errors["source"] = FileError.LARGE
        } else if (
            source.fileName == null
            || !source.fileName.matches(FILE_NAME_REGEX.toRegex())
            || !source.mimeType!!.startsWith("video")
            ) {
            errors["source"] = FileError.NOT_SUPPORTED
        }
        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }
        return videoService.save(
            userId = userId,
            video = VideoCreationModel(
                channelId = video.channelId!!,
                title = video.title!!,
                description = video.description,
                cover = cover?.toUploadedFile(),
                source = source.toDomain()
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
        input.use {
            videoService.saveVideoSource(
                userId = userId,
                tmpId = UUID.fromString(tmpId!!),
                start = start,
                end = end,
                source = it
            )
        }
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
        val errors = mutableMapOf<String, FileError>()
        if (video.coverAction == EditingActionDto.EDIT && cover == null) {
            errors["cover"] = FileError.EMPTY
        }
        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }
        return videoService.edit(
            userId = userId,
            video = VideoEditingModel(
                videoId = video.videoId!!,
                title = video.title!!,
                description = video.description,
                cover = when (video.coverAction) {
                    EditingActionDto.KEEP -> EditingAction.Keep
                    EditingActionDto.REMOVE -> EditingAction.Remove
                    EditingActionDto.EDIT -> EditingAction.Edit(cover!!.toUploadedFile())
                }
            )
        ).toDto()
    }

    @DeleteMapping("/{video_id}")
    fun delete(
        @PathVariable("video_id") @LongId videoId: Long?,
        @AuthenticationPrincipal userId: Long
    ) {
        videoService.remove(
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

data class VideoMetaDataDto(
    val fileName: String?,
    val mimeType: String?,
    val size: Long?
)

fun VideoMetaDataDto.toDomain() = VideoMetaData(
    fileName = fileName,
    mimeType = mimeType,
    size = size
)

fun VideoMetaData.toDto() = VideoMetaDataDto(
    fileName = fileName,
    mimeType = mimeType,
    size = size
)

data class VideoEditingRequest(
    @field:LongId
    val videoId: Long?,
    @field:Title
    val title: String?,
    val coverAction: EditingActionDto = EditingActionDto.KEEP,
    @field:Description
    val description: String?
)
