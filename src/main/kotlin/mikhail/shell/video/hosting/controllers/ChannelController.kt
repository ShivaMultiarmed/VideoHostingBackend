package mikhail.shell.video.hosting.controllers

import jakarta.servlet.http.HttpServletRequest
import mikhail.shell.video.hosting.domain.Channel
import mikhail.shell.video.hosting.domain.SubscriptionState
import mikhail.shell.video.hosting.dto.ChannelDto
import mikhail.shell.video.hosting.dto.ChannelWithUserDto
import mikhail.shell.video.hosting.dto.toDto
import mikhail.shell.video.hosting.service.ChannelService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.File

@RestController
@RequestMapping("/api/v1/channels")
class ChannelController @Autowired constructor(
    private val channelService: ChannelService
) {
    @GetMapping("/{channelId}/details")
    fun provideChannelInfo(
        request: HttpServletRequest,
        @RequestParam userId: Long,
        @PathVariable channelId: Long
    ): ResponseEntity<ChannelWithUserDto> {
        val channelForUser = channelService.provideChannelForUser(channelId, userId)
        val channelDto = channelForUser.toDto(
            avatarUrl = "http://${request.localAddr}:${request.localPort}/api/v1/channels/${channelForUser.channelId}/avatar",
            coverUrl = "http://${request.localAddr}:${request.localPort}/api/v1/channels/${channelForUser.channelId}/cover"
        )
        return ResponseEntity.status(HttpStatus.OK).body(channelDto)
    }

    @GetMapping("/{channelId}/cover")
    fun provideChannelCover(
        @PathVariable channelId: Long
    ): ResponseEntity<ByteArray> {
        return try {
            val image = File("D:/VideoHostingStorage/channels/covers/$channelId.png")
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
            val image = File("D:/VideoHostingStorage/channels/avatars/$channelId.png")
            if (!image.exists()) {
                ResponseEntity.status(HttpStatus.NOT_FOUND).build<ByteArray>()
            }
            ResponseEntity.status(HttpStatus.OK).contentType(MediaType.IMAGE_PNG).body(image.inputStream().readAllBytes())
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
    private fun constructChannelDto(
        channel: Channel,
        request: HttpServletRequest
    ): ChannelDto {
        val channelId = channel.channelId
        return channel.toDto(
            avatarUrl = "http://${request.localAddr}:${request.localPort}/api/v1/channels/${channel.channelId}/avatar",
            coverUrl = "http://${request.localAddr}:${request.localPort}/api/v1/channels/${channel.channelId}/cover"
        )
    }
}