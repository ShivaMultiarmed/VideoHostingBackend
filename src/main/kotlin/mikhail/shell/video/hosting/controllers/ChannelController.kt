package mikhail.shell.video.hosting.controllers

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import mikhail.shell.video.hosting.domain.*
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
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/channels")
class ChannelController @Autowired constructor(
    private val channelService: ChannelService
) {
    @Value("\${video-hosting.server.base-url}")
    private lateinit var BASE_URL: String

    @GetMapping("/{channelId}")
    fun get(@PathVariable @Positive channelId: Long): ChannelDto {
        return channelService.getChannel(channelId).toDto()
    }

    @GetMapping("/{channelId}/details")
    fun getDetails(
        @PathVariable @Positive channelId: Long,
        @AuthenticationPrincipal userId: Long
    ): ChannelWithUserDto {
        return channelService.getForUser(channelId = channelId, userId = userId).toDto()
    }

    @GetMapping("/{channelId}/header")
    fun getHeader(@PathVariable @Positive channelId: Long): ResponseEntity<Resource> {
        val image = channelService.getHeader(channelId)
        return ResponseEntity.status(HttpStatus.OK)
            .contentType(MediaType.parseMediaType("image/${image.file.extension}"))
            .body(image)
    }

    @GetMapping("/{channelId}/logo")
    fun getLogo(@PathVariable @Positive channelId: Long): ResponseEntity<Resource> {
        val image = channelService.getLogo(channelId)
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.parseMediaType("image/${image.file.extension}"))
                .body(image)
    }

    @PostMapping(consumes = ["multipart/form-data"])
    fun createChannel(
        @Validated @ModelAttribute channel: ChannelCreationRequest,
        @AuthenticationPrincipal userId: Long,
    ): ChannelDto {
        return channelService.createChannel(
            channel = Channel(
                ownerId = userId,
                title = channel.title,
                alias = channel.alias,
                description = channel.description
            ),
            logo = channel.logo?.toUploadedFile(),
            header = channel.header?.toUploadedFile()
        ).toDto()
    }

    @PatchMapping(consumes = ["multipart/form-data"])
    fun editChannel(
        @Validated @ModelAttribute channel: ChannelEditingRequest,
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
            header = channel.header?.toUploadedFile(),
            headerAction = channel.editHeaderAction,
            logo = channel.logo?.toUploadedFile(),
            logoAction = channel.editHeaderAction,
        ).toDto()
    }

    @GetMapping("/owner/{userId}")
    fun getAllChannelsByOwnerId(@PathVariable @Positive userId: Long): List<ChannelDto> {
        return channelService.getChannelsByOwnerId(userId).map { it.toDto() }
    }

    @GetMapping("/subscriptions")
    fun getAllChannelsBySubscriberId(
        @Positive @Max(Long.MAX_VALUE) partIndex: Long = 0,
        @Positive @Max(Int.MAX_VALUE.toLong()) partSize: Int = 10,
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
        @PathVariable @Positive channelId: Long,
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

    @PostMapping("/notifications/subscribe")
    fun resubscribeToFCM(
        @RequestParam token: String,
        @AuthenticationPrincipal userId: Long
    ) {
        channelService.subscribeToNotifications(userId, token)
    }

    @DeleteMapping("/notifications/subscription")
    fun unsubscribeFromFCM(
        @RequestParam token: String,
        @AuthenticationPrincipal userId: Long
    ) {
        channelService.unsubscribeFromNotifications(userId = userId, token = token)
    }

    @DeleteMapping("/{channelId}")
    fun remove(
        @PathVariable channelId: Long,
        @AuthenticationPrincipal userId: Long
    ) {
        channelService.removeChannel(channelId)
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
    @field:NotBlank
    @field:Max(MAX_TITLE_LENGTH.toLong())
    val title: String,
    @field:NotBlank
    @field:Max(MAX_TITLE_LENGTH.toLong())
    val alias: String?,
    @field:FileValidation(
        max = ValidationRules.MAX_IMAGE_SIZE.toLong(),
        mime = "image"
    )
    val logo: MultipartFile?,
    @field:FileValidation(
        max = ValidationRules.MAX_IMAGE_SIZE.toLong(),
        mime = "image"
    )
    val header: MultipartFile?,
    @field:Max(MAX_TEXT_LENGTH.toLong())
    val description: String?
)

data class ChannelEditingRequest(
    @field:Positive
    val channelId: Long,
    @field:NotBlank
    @field:Max(MAX_TITLE_LENGTH.toLong())
    val title: String,
    @field:NotBlank
    @field:Max(MAX_TITLE_LENGTH.toLong())
    val alias: String?,
    val editLogoAction: EditAction,
    @field:FileValidation(
        max = ValidationRules.MAX_IMAGE_SIZE.toLong(),
        mime = "image"
    )
    val logo: MultipartFile?,
    val editHeaderAction: EditAction,
    @field:FileValidation(
        max = ValidationRules.MAX_IMAGE_SIZE.toLong(),
        mime = "image"
    )
    val header: MultipartFile?,
    @field:Max(MAX_TEXT_LENGTH.toLong())
    val description: String?
)
