package mikhail.shell.video.hosting.controllers

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.domain.ApplicationPaths.VIDEOS_COVERS_BASE_PATH
import mikhail.shell.video.hosting.domain.ApplicationPaths.VIDEOS_PLAYABLES_BASE_PATH
import mikhail.shell.video.hosting.dto.*
import mikhail.shell.video.hosting.service.ChannelService
import mikhail.shell.video.hosting.service.UserService
import mikhail.shell.video.hosting.service.VideoService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.data.repository.query.Param
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.time.Instant

@RestController
@RequestMapping("/api/v2/videos")
class VideoController @Autowired constructor(
    private val videoService: VideoService,
    private val channelService: ChannelService,
    private val userService: UserService
) {
    @Value("\${video-hosting.server.base-url}")
    private lateinit var BASE_URL: String

    @GetMapping("/{videoId}")
    fun get(@PathVariable @Positive videoId: Long): VideoDto {
        return videoService.get(videoId).toDto()
    }

    @GetMapping("/{videoId}/details")
    fun getVideoDetails(
        @PathVariable @Positive videoId: Long,
        @AuthenticationPrincipal userId: Long
    ): VideoDetailsDto {
        val videoDto = videoService.get(videoId = videoId, userId = userId).toDto(
            sourceUrl = "$BASE_URL/videos/${videoId}/play",
            coverUrl = "$BASE_URL/videos/${videoId}/cover"
        )
        val channelDto = channelService.getForUser(videoDto.channelId, userId).toDto(
            avatarUrl = "$BASE_URL/channels/${videoDto.channelId}/avatar",
            coverUrl = "$BASE_URL/channels/${videoDto.channelId}/cover"
        )
        return VideoDetailsDto(
            video = videoDto,
            channel = channelDto
        )
    }

    @PatchMapping("/{videoId}/rate")
    fun rateVideo(
        @PathVariable videoId: Long,
        @RequestParam liking: Liking,
        @AuthenticationPrincipal userId: Long
    ): VideoWithUserDto {
        return videoService.rate(videoId = videoId, userId = userId, liking = liking).toDto(
            sourceUrl = "$BASE_URL/videos/${videoId}/play",
            coverUrl = "$BASE_URL/videos/${videoId}/cover"
        )
    }

    @GetMapping(path = ["/{videoId}/play", "/{videoId}/download"])
    fun playVideo(
        @PathVariable videoId: Long,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        val sourcesDirectory = File(VIDEOS_PLAYABLES_BASE_PATH)
        val file = findFileByName(sourcesDirectory, videoId.toString())
        if (file?.exists() != true || !videoService.checkExistence(videoId)) {
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

    @GetMapping("/channel/{channelId}")
    fun provideVideosFromChannel(
        @PathVariable @Positive channelId: Long,
        @Param("partSize") @Min(1) @Max(100) partSize: Int = 10,
        @Param("partNumber") @Min(0) @Max(Long.MAX_VALUE) partNumber: Long = 0
    ): List<VideoDto> {
        return videoService
            .getByChannelId(
                channelId = channelId,
                partSize = partSize,
                partNumber = partNumber
            ).map {
                it.toDto(
                    sourceUrl = "$BASE_URL/videos/${it.videoId}/play",
                    coverUrl = "$BASE_URL/videos/${it.videoId}/cover"
                )
            }
    }

    @GetMapping("/{videoId}/cover")
    fun provideVideoCover(@PathVariable @Positive videoId: Long): ResponseEntity<Resource> {
        val image = videoService.getCover(videoId)
        return ResponseEntity.status(HttpStatus.OK)
            .contentType(MediaType.parseMediaType("image/${image.file.extension}"))
            .body(image)
    }

    @GetMapping("/search")
    fun search(
        @RequestParam @NotBlank @Max(ValidationRules.MAX_TITLE_LENGTH.toLong()) query: String,
        @RequestParam @Min(1) @Max(100) partSize: Int = 10,
        @RequestParam @Min(0) @Max(Long.MAX_VALUE) partNumber: Long = 0
    ): List<VideoWithChannelDto> {
        return videoService.getByQuery(
            query = query,
            partSize = partSize,
            partNumber = partNumber
        ).map {
            VideoWithChannelDto(
                video = it.video.toDto(
                    sourceUrl = "$BASE_URL/videos/${it.video.videoId}/play",
                    coverUrl = "$BASE_URL/videos/${it.video.videoId}/cover"
                ),
                channel = it.channel.toDto(
                    avatarUrl = "$BASE_URL/channels/${it.channel.channelId}/avatar",
                    coverUrl = "$BASE_URL/channels/${it.channel.channelId}/cover"
                )
            )
        }
    }

    @PostMapping
    fun upload(
        @Validated @ModelAttribute request: VideoCreationRequest,
        @AuthenticationPrincipal userId: Long
    ): VideoDto {
        return videoService.save(
            userId = userId,
            video = Video(
                channelId = request.channelId,
                title = request.title,
                dateTime = Instant.now()
            ),
            cover = request.cover?.toUploadedFile()
        ).let {
            it.toDto(
                sourceUrl = "$BASE_URL/videos/${it.videoId}/play",
                coverUrl = "$BASE_URL/videos/${it.videoId}/cover"
            )
        }
    }

    @PostMapping("/{videoId}/source", consumes = ["application/octet-stream"])
    fun uploadSource(
        @PathVariable videoId: Long,
        @RequestParam extension: String,
        input: InputStream,
        @AuthenticationPrincipal userId: Long
    ): ResponseEntity<Unit> {
        if (!videoService.checkOwner(userId, videoId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        if (!videoService.checkExistence(videoId)) {
            throw NoSuchElementException()
        }
        val fileName = "source.$extension"
        val result = videoService.saveVideoSource(
            videoId,
            input
        )
        return if (result) {
            ResponseEntity.status(HttpStatus.CREATED).build()
        } else {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @PostMapping("/{videoId}/confirm")
    fun confirmVideoUpload(
        @PathVariable @Positive videoId: Long,
        @AuthenticationPrincipal userId: Long
    ) = videoService.confirm(userId = userId, videoId = videoId)

    @PatchMapping("/{videoId}/increment-views")
    fun incrementViews(@PathVariable @Positive videoId: Long) = videoService.incrementViews(videoId).toDto()

    @PatchMapping("/edit")
    fun editVideo(
        @Validated @ModelAttribute request: VideoEditingRequest,
        @AuthenticationPrincipal userId: Long
    ): VideoDto {
        return videoService.edit(
            userId = userId,
            video = Video(
                videoId = request.videoId,
                title = request.title,
                channelId = request.channelId,
            ),
            coverAction = request.coverAction,
            cover = request.cover?.toUploadedFile()
        ).let {
            it.toDto(
                sourceUrl = "$BASE_URL/videos/${it.videoId}/play",
                coverUrl = "$BASE_URL/videos/${it.videoId}/cover"
            )
        }
    }

    @DeleteMapping("/{videoId}")
    fun deleteVideo(
        @PathVariable @Positive videoId: Long,
        @AuthenticationPrincipal userId: Long
    ) {
        videoService.delete(userId = userId, videoId = videoId)
    }

    @GetMapping("/recommendations")
    fun getRecommendations(
        @RequestParam @Min(0) @Max(Long.MAX_VALUE) partIndex: Long = 0,
        @RequestParam @Min(1) @Max(100) partSize: Int = 10,
        @AuthenticationPrincipal userId: Long
    ): List<VideoWithChannelDto> {
        return videoService
            .getRecommendations(
                userId,
                partIndex,
                partSize
            ).map {
                VideoWithChannelDto(
                    video = it.video.toDto(
                        sourceUrl = "$BASE_URL/videos/${it.video.videoId}/play",
                        coverUrl = "$BASE_URL/videos/${it.video.videoId}/cover"
                    ),
                    channel = it.channel.toDto(
                        avatarUrl = "$BASE_URL/channels/${it.channel.channelId}/avatar",
                        coverUrl = "$BASE_URL/channels/${it.channel.channelId}/cover"
                    )
                )
            }
    }

    private fun Video.toDto() = toDto(
        sourceUrl = "$BASE_URL/videos/${videoId}/play",
        coverUrl = "$BASE_URL/videos/${videoId}/cover"
    )

    private fun VideoWithUser.toDto() = toDto(
        sourceUrl = "$BASE_URL/videos/${videoId}/play",
        coverUrl = "$BASE_URL/videos/${videoId}/cover"
    )
}

data class VideoCreationRequest(
    @field:NotBlank @field:Max(ValidationRules.MAX_TITLE_LENGTH.toLong())
    val title: String,
    @field:Positive
    val channelId: Long,
    @field:FileValidation(
        max = ValidationRules.MAX_IMAGE_SIZE.toLong(),
        mime = "image"
    )
    val cover: MultipartFile?,
    @field:NotBlank @field:Max(ValidationRules.MAX_TEXT_LENGTH.toLong())
    val description: String?
)

data class VideoEditingRequest(
    @field:Positive
    val videoId: Long,
    @field:NotBlank @field:Max(ValidationRules.MAX_TITLE_LENGTH.toLong())
    val title: String,
    @field:Positive
    val channelId: Long,
    @field:FileValidation(
        max = ValidationRules.MAX_IMAGE_SIZE.toLong(),
        mime = "image"
    )
    val cover: MultipartFile?,
    val coverAction: EditAction,
    @field:NotBlank @field:Max(ValidationRules.MAX_TEXT_LENGTH.toLong())
    val description: String?
)
