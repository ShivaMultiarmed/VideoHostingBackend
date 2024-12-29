package mikhail.shell.video.hosting.controllers

import jakarta.servlet.http.HttpServletRequest
import mikhail.shell.video.hosting.domain.ApplicationPaths.CHANNEL_AVATARS_BASE_PATH
import mikhail.shell.video.hosting.domain.ApplicationPaths.CHANNEL_COVERS_BASE_PATH
import mikhail.shell.video.hosting.domain.SubscriptionState
import mikhail.shell.video.hosting.domain.findFileByName
import mikhail.shell.video.hosting.domain.parseExtension
import mikhail.shell.video.hosting.dto.ChannelDto
import mikhail.shell.video.hosting.dto.ChannelWithUserDto
import mikhail.shell.video.hosting.dto.toDomain
import mikhail.shell.video.hosting.dto.toDto
import mikhail.shell.video.hosting.service.ChannelService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.File

@RestController
@RequestMapping("/api/v1/channels")
class ChannelController @Autowired constructor(
    private val channelService: ChannelService
) {
	private val IP = "158.160.22.54"
    @GetMapping("/{channelId}/details")
    fun provideChannelInfo(
        request: HttpServletRequest,
        @RequestParam userId: Long,
        @PathVariable channelId: Long
    ): ResponseEntity<ChannelWithUserDto> {
        val channelForUser = channelService.provideChannelForUser(channelId, userId)
        val channelDto = channelForUser.toDto(
            avatarUrl = "http://$IP:${request.localPort}/api/v1/channels/${channelForUser.channelId}/avatar",
            coverUrl = "http://$IP:${request.localPort}/api/v1/channels/${channelForUser.channelId}/cover"
        )
        return ResponseEntity.status(HttpStatus.OK).body(channelDto)
    }

    @GetMapping("/{channelId}/cover")
    fun provideChannelCover(
        @PathVariable channelId: Long
    ): ResponseEntity<ByteArray> {
        return try {
            val coverFolder = File(CHANNEL_COVERS_BASE_PATH)
            val image = findFileByName(coverFolder, channelId.toString())
            if (image?.exists() != true) {
                ResponseEntity.status(HttpStatus.NOT_FOUND).build<ByteArray>()
            }
            ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.parseMediaType("image/${image?.name?.parseExtension()}"))
                .body(image?.inputStream()?.readAllBytes())
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/{channelId}/avatar")
    fun provideChannelAvatar(
        @PathVariable channelId: Long
    ): ResponseEntity<ByteArray> {
        return try {
            val avatarFolder = File(CHANNEL_AVATARS_BASE_PATH)
            val image = findFileByName(avatarFolder, channelId.toString())
            if (image?.exists() != true) {
                ResponseEntity.status(HttpStatus.NOT_FOUND).build<ByteArray>()
            }
            ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.parseMediaType("image/${image?.name?.parseExtension()}"))
                .body(image?.inputStream()?.readAllBytes())
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @PostMapping(
        path = ["/create"],
        consumes = ["multipart/form-data"]
    )
    fun createChannel(
        request: HttpServletRequest,
        @RequestPart("channel") channel: ChannelDto,
        @RequestPart("cover") coverFile: MultipartFile?,
        @RequestPart("avatar") avatarFile: MultipartFile?
    ): ResponseEntity<ChannelDto> {
        val cover = coverFile?.let {
            mikhail.shell.video.hosting.domain.File(
                it.originalFilename,
                it.contentType,
                it.bytes
            )
        }
        val avatar = avatarFile?.let {
            mikhail.shell.video.hosting.domain.File(
                it.originalFilename,
                it.contentType,
                it.bytes
            )
        }
        val createdChannel = channelService.createChannel(
            channel = channel.toDomain(),
            cover = cover,
            avatar = avatar
        )
        return ResponseEntity.status(HttpStatus.OK).body(createdChannel.toDto())
    }

    @GetMapping("/owner/{userId}")
    fun getAllChannelsByOwnerId(
        request: HttpServletRequest,
        @PathVariable userId: Long
    ): ResponseEntity<List<ChannelDto>> {
        val channels = channelService.getChannelsByOwnerId(userId)
        val channelDtos = channels.map {
            it.toDto(
                avatarUrl = "http://$IP:${request.localPort}/api/v1/channels/${it.channelId}/avatar",
                coverUrl = "http://$IP:${request.localPort}/api/v1/channels/${it.channelId}/cover"
            )
        }
        return ResponseEntity.status(HttpStatus.OK).body(channelDtos)
    }

    @GetMapping("/subscriber/{userId}")
    fun getAllChannelsBySubscriberId(
        request: HttpServletRequest,
        @PathVariable userId: Long
    ): ResponseEntity<List<ChannelDto>> {
        val channels = channelService.getChannelsBySubscriberId(userId)
        val channelDtos = channels.map {
            it.toDto(
                avatarUrl = "http://$IP:${request.localPort}/api/v1/channels/${it.channelId}/avatar",
                coverUrl = "http://$IP:${request.localPort}/api/v1/channels/${it.channelId}/cover"
            )
        }
        return ResponseEntity.status(HttpStatus.OK).body(channelDtos)
    }

    @PatchMapping("/{channelId}/subscribe")
    fun subscribeToChannel(
        request: HttpServletRequest,
        @RequestParam userId: Long,
        @PathVariable channelId: Long,
        @RequestParam subscriptionState: SubscriptionState
    ): ResponseEntity<ChannelWithUserDto> {
        val channelWithUser = channelService.changeSubscriptionState(userId, channelId, subscriptionState)
        return ResponseEntity.status(HttpStatus.OK).body(
            channelWithUser.toDto(
                avatarUrl = "http://$IP:${request.localPort}/api/v1/channels/${channelWithUser.channelId}/avatar",
                coverUrl = "http://$IP:${request.localPort}/api/v1/channels/${channelWithUser.channelId}/cover"
            )
        )
    }
}
