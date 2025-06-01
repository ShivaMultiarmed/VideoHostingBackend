package mikhail.shell.video.hosting.controllers

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.domain.ApplicationPaths.VIDEOS_PLAYABLES_BASE_PATH
import mikhail.shell.video.hosting.dto.*
import mikhail.shell.video.hosting.service.ChannelService
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
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.async.AsyncRequestNotUsableException
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile

@RestController
@RequestMapping("/api/v1/videos")
class VideoController @Autowired constructor(
    private val videoService: VideoService,
    private val channelService: ChannelService,
) {
    @Value("\${hosting.server.host}")
    private lateinit var HOST: String

    @GetMapping("/{videoId}")
    fun getVideoDto(
        request: HttpServletRequest,
        @PathVariable videoId: Long
    ): ResponseEntity<VideoDto> {
        val videoInfo = videoService.getVideoInfo(videoId)
        val videoDto = constructVideoDto(videoInfo, request)
        return ResponseEntity.ok(videoDto)
    }

    @GetMapping("/{videoId}/details")
    fun getVideoDetails(
        request: HttpServletRequest,
        @PathVariable videoId: Long,
        @RequestParam userId: Long
    ): ResponseEntity<VideoDetailsDto> {
        val videoDto = videoService.getVideoForUser(videoId, userId).toDto(
            sourceUrl = "https://$HOST:${request.localPort}/api/v1/videos/${videoId}/play",
            coverUrl = "https://$HOST:${request.localPort}/api/v1/videos/${videoId}/cover"
        )
        val channelDto = channelService.provideChannelForUser(videoDto.channelId, userId).toDto(
            avatarUrl = "https://$HOST:${request.localPort}/api/v1/channels/${videoDto.channelId}/avatar",
            coverUrl = "https://$HOST:${request.localPort}/api/v1/channels/${videoDto.channelId}/cover"
        )
        val videoDetailsDto = VideoDetailsDto(
            video = videoDto,
            channel = channelDto
        )
        return ResponseEntity.ok(videoDetailsDto)
    }

    @PatchMapping("/{videoId}/rate")
    fun rateVideo(
        request: HttpServletRequest,
        @PathVariable videoId: Long,
        @RequestParam userId: Long,
        @RequestParam likingState: LikingState
    ): ResponseEntity<VideoDto> {
        val videoInfo = videoService.rate(videoId, userId, likingState)
        val videoDto = constructVideoDto(videoInfo, request)
        return ResponseEntity.ok(videoDto)
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
        if (file?.exists() != true) {
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
        try {
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
        } catch (e: AsyncRequestNotUsableException) {
            println("Client disconnected or stream terminated: ${e.message}")
        }
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).build()
    }

    @GetMapping("/channel/{channelId}")
    fun provideVideosFromChannel(
        request: HttpServletRequest,
        @PathVariable channelId: Long,
        @Param("partSize") partSize: Int = 10,
        @Param("partNumber") partNumber: Long = 0
    ): ResponseEntity<List<VideoDto>> {
        val videoList = videoService.getVideosByChannelId(channelId, partSize, partNumber)
        val videoDtoList = videoList.map {
            it.toDto(
                sourceUrl = "https://$HOST:${request.localPort}/api/v1/videos/${it.videoId}/play",
                coverUrl = "https://$HOST:${request.localPort}/api/v1/videos/${it.videoId}/cover"
            )
        }
        return ResponseEntity.status(HttpStatus.OK).body(videoDtoList)
    }

    @GetMapping("/{videoId}/cover")
    fun provideVideoCover(
        request: HttpServletRequest,
        @PathVariable videoId: Long
    ): ResponseEntity<Resource> {
        return try {
            val coverDirectory = File(ApplicationPaths.VIDEOS_COVERS_BASE_PATH)
            val image = findFileByName(coverDirectory, videoId.toString())
            if (image?.exists() != true) {
                ResponseEntity.status(HttpStatus.NOT_FOUND).build()
            } else {
                val imageResource = FileSystemResource(image)
                ResponseEntity.status(HttpStatus.OK)
                    .contentType(MediaType.parseMediaType("image/${image.name.parseExtension()}"))
                    .body(imageResource)
            }

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    private fun constructVideoDto(
        video: Video,
        request: HttpServletRequest
    ): VideoDto {
        val videoId = video.videoId
        return video.toDto(
            sourceUrl = "https://$HOST:${request.localPort}/api/v1/videos/${videoId}/play",
            coverUrl = "https://$HOST:${request.localPort}/api/v1/videos/${videoId}/cover"
        )
    }

    @GetMapping("/search")
    fun searchForVideos(
        request: HttpServletRequest,
        @RequestParam query: String,
        @RequestParam partSize: Int = 10,
        @RequestParam partNumber: Long = 0
    ): ResponseEntity<List<VideoWithChannelDto>> {
        val videoDtos = videoService.getVideosByQuery(query, partSize, partNumber).map {
            VideoWithChannelDto(
                video = it.video.toDto(
                    sourceUrl = "https://$HOST:${request.localPort}/api/v1/videos/${it.video.videoId}/play",
                    coverUrl = "https://$HOST:${request.localPort}/api/v1/videos/${it.video.videoId}/cover"
                ),
                channel = it.channel.toDto(
                    avatarUrl = "https://$HOST:${request.localPort}/api/v1/channels/${it.channel.channelId}/avatar",
                    coverUrl = "https://$HOST:${request.localPort}/api/v1/channels/${it.channel.channelId}/cover"
                )
            )
        }
        return ResponseEntity.status(HttpStatus.OK).body(videoDtos)
    }

    @PostMapping("/upload", consumes = ["multipart/form-data"])
    fun uploadVideo(
        request: HttpServletRequest,
        @RequestPart("video") videoDto: VideoDto,
        @RequestPart("cover") coverFile: MultipartFile?,
        @RequestPart("source") sourceFile: MultipartFile
    ): ResponseEntity<VideoDto> {
        val userId = SecurityContextHolder.getContext().authentication.principal as Long?
            ?: return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        if (!channelService.checkOwner(userId, videoDto.channelId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        val cover = coverFile?.let {
            File(
                name = it.originalFilename,
                mimeType = "image/${it.originalFilename?.parseExtension()}",
                content = it.bytes
            )
        }
        val source = File(
            name = sourceFile.originalFilename,
            mimeType = "image/${sourceFile.name.parseExtension()}",
            content = sourceFile.bytes
        )
        val video = videoService.uploadVideo(
            video = videoDto.toDomain(),
            cover = cover,
            source = source
        )
        return ResponseEntity.status(HttpStatus.OK).body(
            video.toDto(
                sourceUrl = "https://$HOST:${request.localPort}/api/v1/videos/${video.videoId}/play",
                coverUrl = "https://$HOST:${request.localPort}/api/v1/videos/${video.videoId}/cover"
            )
        )
    }

    @PostMapping("/upload/details")
    fun uploadVideoDetails(
        request: HttpServletRequest,
        @RequestBody video: VideoDto
    ): ResponseEntity<VideoDto> {
        val userId = SecurityContextHolder.getContext().authentication.principal as Long?
            ?: return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        if (!channelService.checkOwner(userId, video.channelId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        return ResponseEntity.status(HttpStatus.OK)
            .body(
                videoService.saveVideoDetails(
                    video.toDomain()
                ).toDto(
                    sourceUrl = "https://$HOST:${request.localPort}/api/v1/videos/${video.videoId}/play",
                    coverUrl = "https://$HOST:${request.localPort}/api/v1/videos/${video.videoId}/cover"
                )
            )
    }

    @PostMapping("/upload/{videoId}/source", consumes = ["application/octet-stream"])
    fun uploadVideoSource(
        @PathVariable videoId: Long,
        @RequestParam chunkNumber: Int,
        @RequestParam extension: String,
        input: InputStream
    ): ResponseEntity<Boolean> {
        val userId = SecurityContextHolder.getContext().authentication.principal as Long?
            ?: return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        if (!videoService.checkOwner(userId, videoId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        val result = videoService.saveVideoSource(videoId, extension, input)
        return ResponseEntity.status(HttpStatus.OK).body(result)
    }

    @PostMapping("/upload/{videoId}/cover", consumes = ["application/octet-stream"])
    fun uploadVideoCover(
        @PathVariable videoId: Long,
        @RequestParam extension: String,
        input: InputStream
    ): ResponseEntity<Boolean> {
        val userId = SecurityContextHolder.getContext().authentication.principal as Long?
            ?: return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        if (!videoService.checkOwner(userId, videoId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        val result = videoService.saveVideoCover(videoId, extension, input)
        return ResponseEntity.status(HttpStatus.OK).body(result)
    }
    @PostMapping("/upload/{videoId}/confirm")
    fun confirmVideoUpload(
        @PathVariable videoId: Long
    ): ResponseEntity<Boolean> {
        val userId = SecurityContextHolder.getContext().authentication.principal as Long?
            ?: return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        if (!videoService.checkOwner(userId, videoId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        val result = videoService.confirmVideoUpload(videoId)
        return ResponseEntity.status(HttpStatus.OK).body(result)
    }

    @PatchMapping("/{videoId}/increment-views")
    fun incrementViews(
        @PathVariable videoId: Long
    ): Long {
        return videoService.incrementViews(videoId)
    }

    @PatchMapping("/edit")
    fun editVideo(
        request: HttpServletRequest,
        @RequestPart video: VideoDto,
        @RequestPart coverAction: EditAction,
        @RequestPart(required = false) cover: MultipartFile? = null
    ): ResponseEntity<VideoDto> {
        val userId = SecurityContextHolder.getContext().authentication.principal as Long?
            ?: return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        if (!videoService.checkOwner(userId, video.videoId!!)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        val coverFile = cover?.let {
            File(
                name = cover.originalFilename,
                mimeType = cover.contentType,
                content = cover.bytes
            )
        }
        val updatedVideo = videoService.editVideo(video.toDomain(), coverAction, coverFile)
        return ResponseEntity.status(HttpStatus.OK).body(
            updatedVideo.toDto(
                sourceUrl = "https://$HOST:${request.localPort}/api/v1/videos/${updatedVideo.videoId}/play",
                coverUrl = "https://$HOST:${request.localPort}/api/v1/videos/${updatedVideo.videoId}/cover"
            )
        )
    }

    @DeleteMapping("/{videoId}")
    fun deleteVideo(
        request: HttpServletRequest,
        @PathVariable videoId: Long
    ): ResponseEntity<Void> {
        val userId = SecurityContextHolder.getContext().authentication.principal as Long?
            ?: return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        if (!videoService.checkOwner(userId, videoId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        val result = videoService.deleteVideo(videoId)
        return if (result) ResponseEntity.status(HttpStatus.OK).build()
        else ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
    }

    @PatchMapping("/search/sync")
    fun syncSearchIndexes(): ResponseEntity<Void> {
        videoService.sync()
        return ResponseEntity.status(HttpStatus.OK).build()
    }
}
