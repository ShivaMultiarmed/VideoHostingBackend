package mikhail.shell.video.hosting.controllers

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.domain.ApplicationPaths.VIDEOS_PLAYABLES_BASE_PATH
import mikhail.shell.video.hosting.dto.*
import mikhail.shell.video.hosting.errors.*
import mikhail.shell.video.hosting.service.ChannelService
import mikhail.shell.video.hosting.service.UserService
import mikhail.shell.video.hosting.service.VideoService
import mikhail.shell.video.hosting.utils.getMimeType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.data.repository.query.Param
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile

@RestController
@RequestMapping("/api/v1/videos")
class VideoController @Autowired constructor(
    private val videoService: VideoService,
    private val channelService: ChannelService,
    private val userService: UserService
) {
    @Value("\${hosting.server.host}")
    private lateinit var HOST: String

    @GetMapping("/{videoId}")
    fun getVideoDto(
        request: HttpServletRequest,
        @PathVariable videoId: Long
    ): ResponseEntity<VideoDto> {
        val videoInfo = videoService.getVideoInfo(videoId)
        val videoDto = videoInfo.toDto()
        return ResponseEntity.ok(videoDto)
    }

    @GetMapping("/{videoId}/details")
    fun getVideoDetails(
        @PathVariable videoId: Long
    ): ResponseEntity<VideoDetailsDto> {
        if (!videoService.checkExistence(videoId)) {
            throw NoSuchElementException()
        }
        val userId = SecurityContextHolder.getContext().authentication.principal as Long?
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        if (!userService.checkExistence(userId)) {
            throw IllegalAccessException()
        }
        val videoDto = videoService.getVideoForUser(videoId, userId).toDto(
            sourceUrl = "https://${constructReferenceBaseApiUrl(HOST)}/videos/${videoId}/play",
            coverUrl = "https://${constructReferenceBaseApiUrl(HOST)}/videos/${videoId}/cover"
        )
        val channelDto = channelService.provideChannelForUser(videoDto.channelId, userId).toDto(
            avatarUrl = "https://${constructReferenceBaseApiUrl(HOST)}/channels/${videoDto.channelId}/avatar",
            coverUrl = "https://${constructReferenceBaseApiUrl(HOST)}/channels/${videoDto.channelId}/cover"
        )
        val videoDetailsDto = VideoDetailsDto(
            video = videoDto,
            channel = channelDto
        )
        return ResponseEntity.ok(videoDetailsDto)
    }

    @PatchMapping("/{videoId}/rate")
    fun rateVideo(
        @PathVariable videoId: Long,
        @RequestParam likingState: LikingState
    ): ResponseEntity<Unit> {
        val userId = SecurityContextHolder.getContext().authentication.principal as Long?
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        if (!userService.checkExistence(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        videoService.rate(videoId, userId, likingState)
        return ResponseEntity.status(HttpStatus.OK).build()
    }

    @GetMapping(
        path = ["/{videoId}/play", "/{videoId}/download"]
    )
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
        @PathVariable channelId: Long,
        @Param("partSize") partSize: Int = 10,
        @Param("partNumber") partNumber: Long = 0
    ): ResponseEntity<List<VideoDto>> {
        if (partSize < 1 || partNumber < 0) {
            throw IllegalArgumentException()
        }
        val videoList = videoService.getVideosByChannelId(channelId, partSize, partNumber)
        val videoDtoList = videoList.map {
            it.toDto(
                sourceUrl = "https://${constructReferenceBaseApiUrl(HOST)}/videos/${it.videoId}/play",
                coverUrl = "https://${constructReferenceBaseApiUrl(HOST)}/videos/${it.videoId}/cover"
            )
        }
        return ResponseEntity.status(HttpStatus.OK).body(videoDtoList)
    }

    @GetMapping("/{videoId}/cover")
    fun provideVideoCover(
        request: HttpServletRequest,
        @PathVariable videoId: Long
    ): ResponseEntity<Resource> {
        val coverDirectory = File(ApplicationPaths.VIDEOS_COVERS_BASE_PATH)
        val image = findFileByName(coverDirectory, videoId.toString())
        return if (image?.exists() != true || !videoService.checkExistence(videoId)) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        } else {
            val imageResource = FileSystemResource(image)
            ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.parseMediaType("image/${image.name.parseExtension()}"))
                .body(imageResource)
        }
    }

    @GetMapping("/search")
    fun searchForVideos(
        request: HttpServletRequest,
        @RequestParam query: String,
        @RequestParam partSize: Int = 10,
        @RequestParam partNumber: Long = 0
    ): ResponseEntity<List<VideoWithChannelDto>> {
        if (partSize < 1 || partNumber < 0) {
            throw IllegalArgumentException()
        }
        val videoDtos = videoService.getVideosByQuery(query, partSize, partNumber).map {
            VideoWithChannelDto(
                video = it.video.toDto(
                    sourceUrl = "https://${constructReferenceBaseApiUrl(HOST)}/videos/${it.video.videoId}/play",
                    coverUrl = "https://${constructReferenceBaseApiUrl(HOST)}/videos/${it.video.videoId}/cover"
                ),
                channel = it.channel.toDto(
                    avatarUrl = "https://${constructReferenceBaseApiUrl(HOST)}/channels/${it.channel.channelId}/avatar",
                    coverUrl = "https://${constructReferenceBaseApiUrl(HOST)}/channels/${it.channel.channelId}/cover"
                )
            )
        }
        return ResponseEntity.status(HttpStatus.OK).body(videoDtos)
    }

    @PostMapping("/upload/details")
    fun uploadVideoDetails(@RequestBody video: VideoDto): ResponseEntity<VideoDto> {
        val userId = SecurityContextHolder.getContext().authentication.principal as Long?
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        if (!channelService.checkOwner(userId, video.channelId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        val compoundError = CompoundError<UploadVideoError>()
        if (video.title.isEmpty()) {
            compoundError.add(UploadVideoError.TITLE_EMPTY)
        }
        if (compoundError.isNotNull()) {
            throw ValidationException(compoundError)
        }
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(
                videoService.saveVideoDetails(
                    video.toDomain()
                ).toDto(
                    sourceUrl = "https://${constructReferenceBaseApiUrl(HOST)}/videos/${video.videoId}/play",
                    coverUrl = "https://${constructReferenceBaseApiUrl(HOST)}/videos/${video.videoId}/cover"
                )
            )
    }

    @PostMapping("/upload/{videoId}/source", consumes = ["application/octet-stream"])
    fun uploadVideoSource(
        @PathVariable videoId: Long,
        @RequestParam extension: String,
        input: InputStream
    ): ResponseEntity<Unit> {
        val userId = SecurityContextHolder.getContext().authentication.principal as Long?
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        if (!videoService.checkOwner(userId, videoId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        if (!videoService.checkExistence(videoId)) {
            throw NoSuchElementException()
        }
        val fileName = "source.$extension"
        val result = videoService.saveVideoSource(
            videoId,
            File(
                name = fileName,
                mimeType = getMimeType(fileName),
                content = input.use { it.readAllBytes() }
            )
        )
        return if (result) {
            ResponseEntity.status(HttpStatus.CREATED).build()
        } else {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @PostMapping("/upload/{videoId}/cover", consumes = ["application/octet-stream"])
    fun uploadVideoCover(
        @PathVariable videoId: Long,
        @RequestParam extension: String,
        input: InputStream
    ): ResponseEntity<Unit> {
        val userId = SecurityContextHolder.getContext().authentication.principal as Long?
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        if (!videoService.checkOwner(userId, videoId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        if (!videoService.checkExistence(videoId)) {
            throw NoSuchElementException()
        }
        val fileName = "cover.$extension"
        val result = videoService.saveVideoCover(
            videoId,
            File(
                name = fileName,
                mimeType = getMimeType(fileName),
                content = input.use { it.readAllBytes() }
            )
        )
        return if (result) {
            ResponseEntity.status(HttpStatus.CREATED).build()
        } else {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @PostMapping("/upload/{videoId}/confirm")
    fun confirmVideoUpload(@PathVariable videoId: Long): ResponseEntity<Unit> {
        val userId = SecurityContextHolder.getContext().authentication.principal as Long?
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        if (!videoService.checkOwner(userId, videoId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        videoService.confirmVideoUpload(videoId)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PatchMapping("/{videoId}/increment-views")
    fun incrementViews(@PathVariable videoId: Long) = videoService.incrementViews(videoId)

    @PatchMapping("/edit")
    fun editVideo(
        @RequestPart video: VideoDto,
        @RequestPart coverAction: EditAction,
        @RequestPart cover: MultipartFile? = null
    ): ResponseEntity<VideoDto> {
        val userId = SecurityContextHolder.getContext().authentication.principal as Long?
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        if (video.videoId == null || !videoService.checkExistence(video.videoId)) {
            throw NoSuchElementException()
        }
        if (!videoService.checkOwner(userId, video.videoId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        val coverFile = cover?.let {
            File(
                name = cover.originalFilename,
                mimeType = cover.contentType,
                content = cover.bytes
            )
        }
        val compoundError = CompoundError<EditVideoError>()
        if (video.title.isEmpty()) {
            compoundError.add(EditVideoError.TITLE_EMPTY)
        }
        if (compoundError.isNotNull()) {
            throw ValidationException(compoundError)
        }
        val updatedVideo = videoService.editVideo(video.toDomain(), coverAction, coverFile)
        return ResponseEntity.status(HttpStatus.OK).body(
            updatedVideo.toDto(
                sourceUrl = "https://${constructReferenceBaseApiUrl(HOST)}/videos/${updatedVideo.videoId}/play",
                coverUrl = "https://${constructReferenceBaseApiUrl(HOST)}/videos/${updatedVideo.videoId}/cover"
            )
        )
    }

    @DeleteMapping("/{videoId}")
    fun deleteVideo(
        request: HttpServletRequest,
        @PathVariable videoId: Long
    ): ResponseEntity<Unit> {
        val userId = SecurityContextHolder.getContext().authentication.principal as Long?
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        if (!videoService.checkOwner(userId, videoId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        return if (videoService.deleteVideo(videoId)) {
            ResponseEntity.status(HttpStatus.OK).build()
        } else {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/recommendations")
    fun getRecommendations(
        @RequestParam partIndex: Long = 0,
        @RequestParam partSize: Int = 10
    ): ResponseEntity<List<VideoWithChannelDto>> {
        val userId = SecurityContextHolder.getContext().authentication.principal as Long?
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        if (!userService.checkExistence(userId)) {
            throw NoSuchElementException()
        }
        if (partIndex < 0 || partSize < 1) {
            throw IllegalArgumentException()
        }
        val videoList = videoService
            .getRecommendedVideos(
                userId,
                partIndex,
                partSize
            ).map {
                VideoWithChannelDto(
                    video = it.video.toDto(
                        sourceUrl = "https://${constructReferenceBaseApiUrl(HOST)}/videos/${it.video.videoId}/play",
                        coverUrl = "https://${constructReferenceBaseApiUrl(HOST)}/videos/${it.video.videoId}/cover"
                    ),
                    channel = it.channel.toDto(
                        avatarUrl = "https://${constructReferenceBaseApiUrl(HOST)}/channels/${it.channel.channelId}/avatar",
                        coverUrl = "https://${constructReferenceBaseApiUrl(HOST)}/channels/${it.channel.channelId}/cover"
                    )
                )
            }
        return ResponseEntity.status(HttpStatus.OK).body(videoList)
    }

    private fun Video.toDto() = toDto(
        sourceUrl = "https://${constructReferenceBaseApiUrl(HOST)}/videos/${videoId}/play",
        coverUrl = "https://${constructReferenceBaseApiUrl(HOST)}/videos/${videoId}/cover"
    )
}
