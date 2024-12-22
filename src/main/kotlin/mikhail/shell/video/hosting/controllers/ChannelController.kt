package mikhail.shell.video.hosting.controllers

import jakarta.servlet.http.HttpServletRequest
import mikhail.shell.video.hosting.domain.ChannelInfo
import mikhail.shell.video.hosting.domain.SubscriptionState
import mikhail.shell.video.hosting.dto.ChannelDto
import mikhail.shell.video.hosting.dto.toDto
import mikhail.shell.video.hosting.service.ChannelService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.query.Param
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
    @GetMapping("/{channelId}")
    fun provideChannelInfo(
        request: HttpServletRequest,
        @RequestParam userId: Long? = null,
        @PathVariable channelId: Long
    ): ResponseEntity<ChannelDto> {
        val channelInfo = channelService.provideChannelInfo(channelId)
        val channelDto = constructChannelDto(channelInfo, userId, request)
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
        channelInfo: ChannelInfo,
        userId: Long?,
        request: HttpServletRequest
    ): ChannelDto {
        val channelId = channelInfo.channelId
        val subscriptionState = if (userId != null) {
            when(channelService.checkIfSubscribed(channelId, userId)) {
                true -> SubscriptionState.SUBSCRIBED
                false -> SubscriptionState.NOT_SUBSCRIBED
            }
        } else SubscriptionState.UNKNOWN
        return channelInfo.toDto(
            subscription = subscriptionState,
            avatarUrl = "http://${request.localAddr}:${request.localPort}/api/v1/channels/${channelInfo.channelId}/avatar",
            coverUrl = "http://${request.localAddr}:${request.localPort}/api/v1/videos/${channelInfo.channelId}/cover"
        )
    }
}