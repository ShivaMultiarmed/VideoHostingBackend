package mikhail.shell.video.hosting.controllers

import jakarta.servlet.http.HttpServletRequest
import mikhail.shell.video.hosting.domain.ApplicationPaths.CHANNEL_AVATARS_BASE_PATH
import mikhail.shell.video.hosting.domain.ApplicationPaths.CHANNEL_COVERS_BASE_PATH
import mikhail.shell.video.hosting.domain.EditAction
import mikhail.shell.video.hosting.domain.SubscriptionState
import mikhail.shell.video.hosting.domain.findFileByName
import mikhail.shell.video.hosting.domain.parseExtension
import mikhail.shell.video.hosting.dto.ChannelDto
import mikhail.shell.video.hosting.dto.ChannelWithUserDto
import mikhail.shell.video.hosting.dto.toDomain
import mikhail.shell.video.hosting.dto.toDto
import mikhail.shell.video.hosting.service.ChannelService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import mikhail.shell.video.hosting.domain.File

@RestController
@RequestMapping("/api/v1/channels")
class ChannelController @Autowired constructor(
    private val channelService: ChannelService
) {
    @Value("\${hosting.server.host}")
    private lateinit var HOST: String

    @GetMapping("/{channelId}/details")
    fun provideChannelInfo(
        request: HttpServletRequest,
        @RequestParam userId: Long,
        @PathVariable channelId: Long
    ): ResponseEntity<ChannelWithUserDto> {
        val channelForUser = channelService.provideChannelForUser(channelId, userId)
        val channelDto = channelForUser.toDto(
            avatarUrl = "https://$HOST:${request.localPort}/api/v1/channels/${channelForUser.channelId}/avatar",
            coverUrl = "https://$HOST:${request.localPort}/api/v1/channels/${channelForUser.channelId}/cover"
        )
        return ResponseEntity.status(HttpStatus.OK).body(channelDto)
    }

    @GetMapping("/{channelId}/cover")
    fun provideChannelCover(
        @PathVariable channelId: Long
    ): ResponseEntity<ByteArray> {
        return try {
            val coverFolder = java.io.File(CHANNEL_COVERS_BASE_PATH)
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
            val avatarFolder = java.io.File(CHANNEL_AVATARS_BASE_PATH)
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
            File(
                it.originalFilename,
                it.contentType,
                it.bytes
            )
        }
        val avatar = avatarFile?.let {
            File(
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
        return ResponseEntity.status(HttpStatus.OK).body(
            createdChannel.toDto(
                avatarUrl = "https://$HOST:${request.localPort}/api/v1/channels/${createdChannel.channelId}/avatar",
                coverUrl = "https://$HOST:${request.localPort}/api/v1/channels/${createdChannel.channelId}/cover"
            )
        )
    }

    @PatchMapping(
        path = ["/edit"],
        consumes = ["multipart/form-data"]
    )
    fun editChannel(
        request: HttpServletRequest,
        @RequestPart("channel") channel: ChannelDto,
        @RequestPart("editCoverAction") editCoverAction: EditAction,
        @RequestPart("cover") coverFile: MultipartFile?,
        @RequestPart("editAvatarAction") editAvatarAction: EditAction,
        @RequestPart("avatar") avatarFile: MultipartFile?
    ): ResponseEntity<ChannelDto> {
        val editedChannel = channelService.editChannel(
            channel.toDomain(),
            editCoverAction,
            coverFile?.let {
                File(
                    name = it.originalFilename,
                    mimeType = it.contentType,
                    content = it.bytes
                )
            },
            editAvatarAction,
            avatarFile?.let {
                File(
                    name = it.originalFilename,
                    mimeType = it.contentType,
                    content = it.bytes
                )
            }
        )
        return ResponseEntity.status(HttpStatus.OK).body(
            editedChannel.toDto(
                avatarUrl = "https://$HOST:${request.localPort}/api/v1/channels/${editedChannel.channelId}/avatar",
                coverUrl = "https://$HOST:${request.localPort}/api/v1/channels/${editedChannel.channelId}/cover"
            )
        )
    }

    @GetMapping("/owner/{userId}")
    fun getAllChannelsByOwnerId(
        request: HttpServletRequest,
        @PathVariable userId: Long
    ): ResponseEntity<List<ChannelDto>> {
        val channels = channelService.getChannelsByOwnerId(userId)
        val channelDtos = channels.map {
            it.toDto(
                avatarUrl = "https://$HOST:${request.localPort}/api/v1/channels/${it.channelId}/avatar",
                coverUrl = "https://$HOST:${request.localPort}/api/v1/channels/${it.channelId}/cover"
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
                avatarUrl = "https://$HOST:${request.localPort}/api/v1/channels/${it.channelId}/avatar",
                coverUrl = "https://$HOST:${request.localPort}/api/v1/channels/${it.channelId}/cover"
            )
        }
        return ResponseEntity.status(HttpStatus.OK).body(channelDtos)
    }

    @PatchMapping("/{channelId}/subscribe")
    fun subscribeToChannel(
        request: HttpServletRequest,
        @RequestParam userId: Long,
        @PathVariable channelId: Long,
        @RequestParam token: String,
        @RequestParam subscriptionState: SubscriptionState
    ): ResponseEntity<ChannelWithUserDto> {
        val channelWithUser = channelService.changeSubscriptionState(userId, channelId, token, subscriptionState)
        return ResponseEntity.status(HttpStatus.OK).body(
            channelWithUser.toDto(
                avatarUrl = "https://$HOST:${request.localPort}/api/v1/channels/${channelWithUser.channelId}/avatar",
                coverUrl = "https://$HOST:${request.localPort}/api/v1/channels/${channelWithUser.channelId}/cover"
            )
        )
    }

    @PatchMapping("/resubscribe")
    fun resubscribeToFCM(
        @RequestParam userId: Long,
        @RequestParam token: String
    ): ResponseEntity<Void> {
        channelService.resubscribe(userId, token)
        return ResponseEntity.status(HttpStatus.OK).build()
    }
}
