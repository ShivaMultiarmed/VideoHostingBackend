package mikhail.shell.video.hosting.controllers

import jakarta.servlet.http.HttpServletResponse
import mikhail.shell.video.hosting.domain.ExtendedVideoInfo
import mikhail.shell.video.hosting.domain.VideoInfo
import mikhail.shell.video.hosting.service.VideoService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.File

@RestController
@RequestMapping("/api/v1/videos")
class VideoController @Autowired constructor(
    private val videoService: VideoService
) {
    @GetMapping("/{videoId}")
    fun getVideoInfo(
        @PathVariable videoId: Long
    ) : ResponseEntity<VideoInfo> {
        return ResponseEntity.ok(videoService.getVideoInfo(videoId))
    }
    @GetMapping("/{videoId}/extended")
    fun getVideoInfo(
        @PathVariable videoId: Long,
        @RequestParam userId: Long
    ) : ResponseEntity<ExtendedVideoInfo> {
        return ResponseEntity.ok(videoService.getExtendedVideoInfo(videoId, userId))
    }
    @PatchMapping("/{videoId}/rate")
    fun rateVideo(
        @PathVariable videoId: Long,
        @RequestParam userId: Long,
        @RequestParam liking: Boolean
    ) : ResponseEntity<ExtendedVideoInfo> {
        return ResponseEntity.ok(videoService.rate(videoId, userId, liking))
    }
    @GetMapping("/{videoId}/play")
    fun playVideo(
        @PathVariable videoId: Long,
        response: HttpServletResponse
    ) {
        val file = File("D:/VideoHostingStorage/videos/$videoId.mp4")
        response.contentType = "video/mp4"
        response.setContentLengthLong(file.length())
        file.inputStream().use {
            it.copyTo(response.outputStream)
        }
    }
}