package mikhail.shell.video.hosting.controllers

import mikhail.shell.video.hosting.domain.ChannelInfo
import mikhail.shell.video.hosting.dto.ExtendedChannelInfo
import mikhail.shell.video.hosting.service.ChannelService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.query.Param
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.File

@RestController
@RequestMapping("/api/v1/channels")
class ChannelController @Autowired constructor(
    private val channelService: ChannelService
) {
    @GetMapping("/{channelId}")
    fun provideChannelInfo(
        @PathVariable channelId: Long
    ): ResponseEntity<ChannelInfo> {
        return ResponseEntity.status(HttpStatus.OK).body(channelService.provideChannelInfo(channelId))
    }
    @GetMapping("/{channelId}/extended")
    fun providedExtendedChannelInfo(
        @PathVariable channelId: Long,
        @Param("userId") userId: Long
    ): ResponseEntity<ExtendedChannelInfo> {
        return ResponseEntity.status(HttpStatus.OK).body(channelService.getExtendedChannelInfo(channelId, userId))
    }
    @GetMapping("/{channelId}/cover")
    fun provideChannelCover(
        @PathVariable channelId: Long
    ): ResponseEntity<ByteArray> {
        return try {
            val image = File("D:/VideoHostingStorage/channels/covers/1.png")
            if (!image.exists()) {
                ResponseEntity.status(HttpStatus.NOT_FOUND).build<ByteArray>()
            }
            ResponseEntity.status(HttpStatus.OK).contentType(MediaType.IMAGE_PNG).body(image.inputStream().readAllBytes())
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
    @GetMapping("/{channelId}/avatar")
    fun provideChannelAvatar(
        @PathVariable channelId: Long
    ): ResponseEntity<ByteArray> {
        return try {
            val image = File("D:/VideoHostingStorage/channels/avatars/1.png")
            if (!image.exists()) {
                ResponseEntity.status(HttpStatus.NOT_FOUND).build<ByteArray>()
            }
            ResponseEntity.status(HttpStatus.OK).contentType(MediaType.IMAGE_PNG).body(image.inputStream().readAllBytes())
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
}