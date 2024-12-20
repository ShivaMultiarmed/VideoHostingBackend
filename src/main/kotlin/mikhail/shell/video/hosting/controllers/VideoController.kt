package mikhail.shell.video.hosting.controllers

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mikhail.shell.video.hosting.domain.VideoDetails
import mikhail.shell.video.hosting.dto.ExtendedVideoInfo
import mikhail.shell.video.hosting.domain.VideoInfo
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
import java.net.InetAddress

@RestController
@RequestMapping("/api/v1/videos")
class VideoController @Autowired constructor(
    private val videoService: VideoService,
    private val channelService: ChannelService,
) {
    @GetMapping("/{videoId}")
    fun getVideoInfo(
        request: HttpServletRequest,
        @PathVariable videoId: Long
    ): ResponseEntity<VideoInfo> {
        var videoInfo = videoService.getVideoInfo(videoId)
        //videoInfo.coverUrl
        return ResponseEntity.ok(videoInfo)
    }

    @GetMapping("/{videoId}/extended")
    fun getVideoDetails(
        request: HttpServletRequest,
        @PathVariable videoId: Long,
        @RequestParam userId: Long
    ): ResponseEntity<VideoDetails> {
        var video = videoService.getExtendedVideoInfo(videoId, userId)
        //video = video.insertCoverUrl(request)
        val channel = channelService.getExtendedChannelInfo(videoId, userId)
        return ResponseEntity.ok(VideoDetails(video, channel))
    }

    @PatchMapping("/{videoId}/rate")
    fun rateVideo(
        request: HttpServletRequest,
        @PathVariable videoId: Long,
        @RequestParam userId: Long,
        @RequestParam liking: Boolean
    ): ResponseEntity<ExtendedVideoInfo> {
        var extendedVideoInfo = videoService.rate(videoId, userId, liking)
        // extendedVideoInfo = extendedVideoInfo.insertCoverUrl(request)
        return ResponseEntity.ok(extendedVideoInfo)
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
    ): ResponseEntity<List<VideoInfo>> {
        val videoList = videoService.getVideosByChannelId(channelId, partSize, partNumber)
        return ResponseEntity.status(HttpStatus.OK).body(videoList)
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