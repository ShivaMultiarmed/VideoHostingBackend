package mikhail.shell.video.hosting.controllers

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mikhail.shell.video.hosting.domain.ChannelInfo
import mikhail.shell.video.hosting.domain.LikingState
import mikhail.shell.video.hosting.domain.SubscriptionState
import mikhail.shell.video.hosting.dto.VideoDto
import mikhail.shell.video.hosting.domain.VideoInfo
import mikhail.shell.video.hosting.dto.ChannelDto
import mikhail.shell.video.hosting.dto.VideoDetailsDto
import mikhail.shell.video.hosting.dto.toDto
import mikhail.shell.video.hosting.service.ChannelService
import mikhail.shell.video.hosting.service.VideoService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.query.Param
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.async.AsyncRequestNotUsableException
import java.io.File
import java.io.RandomAccessFile

@RestController
@RequestMapping("/api/v1/videos")
class VideoController @Autowired constructor(
    private val videoService: VideoService,
    private val channelService: ChannelService,
) {
    @GetMapping("/{videoId}")
    fun getVideoDto(
        request: HttpServletRequest,
        @PathVariable videoId: Long,
        @RequestParam userId: String? = null
    ): ResponseEntity<VideoDto> {
        val videoInfo = videoService.getVideoInfo(videoId)
        val videoDto = constructVideoDto(videoInfo, userId, request)
        return ResponseEntity.ok(videoDto)
    }

    @GetMapping("/{videoId}/details")
    fun getVideoDetails(
        request: HttpServletRequest,
        @PathVariable videoId: Long,
        @RequestParam userId: String
    ): ResponseEntity<VideoDetailsDto> {
        val videoInfo = videoService.getVideoInfo(videoId)
        val channelInfo = channelService.provideChannelInfo(videoInfo.channelId)
        val videoDetailsDto = VideoDetailsDto(
            videoDto = constructVideoDto(videoInfo, userId, request),
            channelDto = constructChannelDto(channelInfo, userId, request)
        )
        return ResponseEntity.ok(videoDetailsDto)
    }

    @PatchMapping("/{videoId}/rate")
    fun rateVideo(
        request: HttpServletRequest,
        @PathVariable videoId: Long,
        @RequestParam userId: String,
        @RequestParam likingState: LikingState
    ): ResponseEntity<VideoDto> {
        val videoInfo = videoService.rate(videoId, userId, likingState)
        val videoDto = constructVideoDto(videoInfo, userId, request)
        return ResponseEntity.ok(videoDto)
    }

    @GetMapping("/{videoId}/play")
    fun playVideo(
        @PathVariable videoId: Long,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        val file = File("D:/VideoHostingStorage/videos/playables/$videoId.mp4")
        if (!file.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
        val rangeHeader = request.getHeader(HttpHeaders.RANGE)

        if (rangeHeader == null) {
            response.contentType = "video/mp4"
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
        response.contentType = "video/mp4"
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
        @Param("partNumber") partNumber: Long = 1
    ): ResponseEntity<List<VideoDto>> {
        val videoList = videoService.getVideosByChannelId(channelId, partSize, partNumber)
        val videoDtoList = videoList.map {
            it.toDto(
                liking = LikingState.UNKNOWN,
                sourceUrl = "http://${request.localAddr}:${request.localPort}/api/v1/videos/${it.videoId}/play",
                coverUrl = "http://${request.localAddr}:${request.localPort}/api/v1/videos/${it.videoId}/cover"
            )
        }
        return ResponseEntity.status(HttpStatus.OK).body(videoDtoList)
    }

    @GetMapping("/{videoId}/cover")
    fun provideVideoCover(
        request: HttpServletRequest,
        @PathVariable videoId: Long
    ): ResponseEntity<ByteArray> {
        return try {
            val image = File("D:/VideoHostingStorage/videos/covers/$videoId.png")
            if (!image.exists()) {
                ResponseEntity.status(HttpStatus.NOT_FOUND).build<ByteArray>()
            }
            ResponseEntity.status(HttpStatus.OK).contentType(MediaType.IMAGE_PNG)
                .body(image.inputStream().readAllBytes())
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    private fun constructVideoDto(
        videoInfo: VideoInfo,
        userId: String?,
        request: HttpServletRequest
    ): VideoDto {
        val videoId = videoInfo.videoId
        val likeState = if (userId != null) videoService.checkVideoLikeState(videoId, userId) else LikingState.UNKNOWN
        return videoInfo.toDto(
            liking = likeState,
            sourceUrl = "http://${request.localAddr}:${request.localPort}/api/v1/videos/${videoId}/play",
            coverUrl = "http://${request.localAddr}:${request.localPort}/api/v1/videos/${videoId}/cover"
        )
    }

    private fun constructChannelDto(
        channelInfo: ChannelInfo,
        userId: String?,
        request: HttpServletRequest
    ): ChannelDto {
        val channelId = channelInfo.channelId
        val subscriptionState = if (userId != null) {
            when (channelService.checkIfSubscribed(channelId, userId)) {
                true -> SubscriptionState.SUBSCRIBED
                false -> SubscriptionState.NOT_SUBSCRIBED
            }
        } else SubscriptionState.UNKNOWN
        return channelInfo.toDto(
            subscription = subscriptionState,
            avatarUrl = "http://${request.localAddr}:${request.localPort}/api/v1/channels/${channelInfo.channelId}/avatar",
            coverUrl = "http://${request.localAddr}:${request.localPort}/api/v1/channels/${channelInfo.channelId}/cover"
        )
    }
}


//fun ExtendedVideoInfo.insertCoverUrl(request: HttpServletRequest): ExtendedVideoInfo {
//    return this.copy(
//        videoInfo = this.videoInfo.copy(
//            coverUrl = "http://${request.localAddr}:${request.localPort}/api/v1/videos/${this.videoInfo.videoId}/cover"
//        )
//    )
//}
//
//fun VideoInfo.insertCoverUrl(request: HttpServletRequest): VideoInfo {
//    return this.copy(
//        coverUrl = "http://${request.localAddr}:${request.localPort}/api/v1/videos/${this.videoId}/cover"
//    )
//}