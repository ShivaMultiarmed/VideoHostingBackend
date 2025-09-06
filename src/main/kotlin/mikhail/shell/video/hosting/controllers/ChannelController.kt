package mikhail.shell.video.hosting.controllers

import jakarta.validation.Valid
import jakarta.validation.constraints.*
import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.domain.ValidationRules.MAX_IMAGE_SIZE
import mikhail.shell.video.hosting.domain.ValidationRules.MAX_TEXT_LENGTH
import mikhail.shell.video.hosting.domain.ValidationRules.MAX_TITLE_LENGTH
import mikhail.shell.video.hosting.dto.ChannelDto
import mikhail.shell.video.hosting.dto.ChannelWithUserDto
import mikhail.shell.video.hosting.dto.toDto
import mikhail.shell.video.hosting.service.ChannelService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v2/channels")
class ChannelController @Autowired constructor(
    private val channelService: ChannelService
) {
    @Value("\${video-hosting.server.base-url}")
    private lateinit var BASE_URL: String

    @GetMapping("/{channelId}")
    fun get(@PathVariable @LongId channelId: Long): ChannelDto {
        return channelService.getChannel(channelId).toDto()
    }

    @GetMapping("/{channelId}/details")
    fun getDetails(
        @PathVariable @LongId channelId: Long,
        @AuthenticationPrincipal userId: Long
    ): ChannelWithUserDto {
        return channelService.getForUser(channelId = channelId, userId = userId).toDto()
    }

    @GetMapping("/{channelId}/header")
    fun getHeader(@PathVariable @LongId channelId: Long): ResponseEntity<Resource> {
        val image = channelService.getHeader(channelId)
        return ResponseEntity.status(HttpStatus.OK)
            .contentType(MediaType.parseMediaType("image/${image.file.extension}"))
            .body(image)
    }

    @GetMapping("/{channelId}/logo")
    fun getLogo(@PathVariable @LongId channelId: Long): ResponseEntity<Resource> {
        val image = channelService.getLogo(channelId)
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.parseMediaType("image/${image.file.extension}"))
                .body(image)
    }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createChannel(
        @RequestPart @Valid channel: ChannelCreationRequest,
        @RequestPart @Image logo: MultipartFile?,
        @RequestPart @Image header: MultipartFile?,
        @AuthenticationPrincipal userId: Long,
    ): ChannelDto {
        return channelService.createChannel(
            channel = Channel(
                ownerId = userId,
                title = channel.title,
                alias = channel.alias,
                description = channel.description
            ),
            logo = logo?.toUploadedFile(),
            header = header?.toUploadedFile()
        ).toDto()
    }

    @PatchMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun editChannel(
        @RequestPart @Valid channel: ChannelEditingRequest,
        @RequestPart @Image logo: MultipartFile?,
        @RequestPart @Image header: MultipartFile?,
        @AuthenticationPrincipal userId: Long
    ): ChannelDto {
        return channelService.editChannel(
            channel = Channel(
                channelId = channel.channelId,
                ownerId = userId,
                title = channel.title,
                alias = channel.alias,
                description = channel.description
            ),
            header = header?.toUploadedFile(),
            headerAction = channel.editHeaderAction,
            logo = logo?.toUploadedFile(),
            logoAction = channel.editHeaderAction,
        ).toDto()
    }

    @GetMapping("/owners/{userId}")
    fun getAllChannelsByOwnerId(
        @PathVariable @LongId userId: Long,
        @RequestParam @Positive partIndex: Long,
        @RequestParam @Min(1) @Max(100) partSize: Int
    ): List<ChannelDto> {
        return channelService.getChannelsByOwnerId(
            userId = userId,
            partIndex = partIndex,
            partSize = partSize
        ).map { it.toDto() }
    }

    @GetMapping("/subscriptions")
    fun getAllChannelsBySubscriberId(
        @Positive partIndex: Long = 0,
        @Positive partSize: Int = 10,
        @AuthenticationPrincipal userId: Long
    ): List<ChannelDto> {
        return channelService.getSubscriptions(
            userId = userId,
            partIndex = partIndex,
            partSize = partSize
        ).map { it.toDto() }
    }

    @PatchMapping("/{channelId}/subscription")
    fun subscribe(
        @PathVariable @LongId channelId: Long,
        @RequestParam subscription: Subscription,
        @RequestParam @NotBlank fcmToken: String,
        @AuthenticationPrincipal userId: Long,
    ): ChannelWithUserDto {
        return channelService.changeSubscriptionState(
            subscriberId = userId,
            channelId = channelId,
            subscription = subscription,
            token = fcmToken
        ).toDto()
    }

    @PostMapping("/notifications/subscription")
    fun resubscribeToFCM(
        @RequestParam @NotBlank token: String,
        @AuthenticationPrincipal userId: Long
    ) {
        channelService.subscribeToNotifications(userId = userId, token = token)
    }

    @DeleteMapping("/notifications/subscription")
    fun unsubscribeFromFCM(
        @RequestParam @NotBlank token: String,
        @AuthenticationPrincipal userId: Long
    ) {
        channelService.unsubscribeFromNotifications(userId = userId, token = token)
    }

    @DeleteMapping("/{channelId}")
    fun remove(
        @PathVariable @LongId channelId: Long,
        @AuthenticationPrincipal userId: Long
    ) {
        channelService.removeChannel(userId = userId, channelId = channelId)
    }

    private fun Channel.toDto(): ChannelDto = toDto(
        avatarUrl = "$BASE_URL/channels/$channelId/avatar",
        coverUrl = "$BASE_URL/channels/$channelId/cover"
    )

    private fun ChannelWithUser.toDto() = toDto(
        avatarUrl = "$BASE_URL/channels/$channelId/avatar",
        coverUrl = "$BASE_URL/channels/$channelId/cover"
    )
}

data class ChannelCreationRequest(
    @field:Title val title: String,
    @field:Title val alias: String?,
    @field:Description val description: String?
)

data class ChannelEditingRequest(
    @field:Positive(message = "LOW") val channelId: Long,
    @field:Title val title: String,
    @field:Title val alias: String?,
    val editLogoAction: EditAction,
    val editHeaderAction: EditAction,
    @field:Description
    val description: String?
)
