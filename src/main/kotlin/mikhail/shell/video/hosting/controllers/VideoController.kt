package mikhail.shell.video.hosting.controllers

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mikhail.shell.video.hosting.domain.ExtendedVideoInfo
import mikhail.shell.video.hosting.domain.VideoInfo
import mikhail.shell.video.hosting.service.VideoService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.async.AsyncRequestNotUsableException
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

@RestController
@RequestMapping("/api/v1/videos")
class VideoController @Autowired constructor(
    private val videoService: VideoService
) {
    @GetMapping("/{videoId}")
    fun getVideoInfo(
        @PathVariable videoId: Long
    ): ResponseEntity<VideoInfo> {
        return ResponseEntity.ok(videoService.getVideoInfo(videoId))
    }

    @GetMapping("/{videoId}/extended")
    fun getVideoInfo(
        @PathVariable videoId: Long,
        @RequestParam userId: Long
    ): ResponseEntity<ExtendedVideoInfo> {
        return ResponseEntity.ok(videoService.getExtendedVideoInfo(videoId, userId))
    }

    @PatchMapping("/{videoId}/rate")
    fun rateVideo(
        @PathVariable videoId: Long,
        @RequestParam userId: Long,
        @RequestParam liking: Boolean
    ): ResponseEntity<ExtendedVideoInfo> {
        return ResponseEntity.ok(videoService.rate(videoId, userId, liking))
    }

    @GetMapping("/{videoId}/play")
    fun playVideo(
        @PathVariable videoId: Long,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        val file = File("D:/VideoHostingStorage/videos/$videoId.mp4")
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
}