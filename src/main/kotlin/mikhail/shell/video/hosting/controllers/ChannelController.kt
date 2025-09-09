package mikhail.shell.video.hosting.controllers

import jakarta.validation.Valid
import jakarta.validation.constraints.*
import mikhail.shell.video.hosting.domain.*
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

    @GetMapping("/{channel_id}")
    fun get(@PathVariable("channel_id") @LongId channelId: Long): ChannelDto {
        return channelService.getChannel(channelId).toDto()
    }

    @GetMapping("/{channel_id}/details")
    fun getDetails(
        @PathVariable("channel_id") @LongId channelId: Long,
        @AuthenticationPrincipal userId: Long
    ): ChannelWithUserDto {
        return channelService.getForUser(channelId = channelId, userId = userId).toDto()
    }

    @GetMapping("/{channel_id}/header")
    fun getHeader(@PathVariable("channel_id") @LongId channelId: Long): ResponseEntity<Resource> {
        val image = channelService.getHeader(channelId)
        return ResponseEntity.status(HttpStatus.OK)
            .contentType(MediaType.parseMediaType("image/${image.file.extension}"))
            .body(image)
    }

    @GetMapping("/{channel_id}/logo")
    fun getLogo(@PathVariable("channel_id") @LongId channelId: Long): ResponseEntity<Resource> {
        val image = channelService.getLogo(channelId)
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.parseMediaType("image/${image.file.extension}"))
                .body(image)
    }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createChannel(
        @RequestPart("channel") @Valid channel: ChannelCreationRequest,
        @RequestPart("logo") @Image logo: MultipartFile?,
        @RequestPart("header") @Image header: MultipartFile?,
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
        @RequestPart("channel") @Valid channel: ChannelEditingRequest,
        @RequestPart("logo") @Image logo: MultipartFile?,
        @RequestPart("header") @Image header: MultipartFile?,
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

    @GetMapping("/owners/{user_id}")
    fun getAllChannelsByOwnerId(
        @PathVariable("user_id") @LongId userId: Long,
        @RequestParam("part_index") @PartIndex partIndex: Long,
        @RequestParam("part_size") @PartSize partSize: Int
    ): List<ChannelDto> {
        return channelService.getChannelsByOwnerId(
            userId = userId,
            partIndex = partIndex,
            partSize = partSize
        ).map { it.toDto() }
    }

    @GetMapping("/subscriptions")
    fun getAllChannelsBySubscriberId(
        @RequestParam("part_index") @PartIndex partIndex: Long,
        @RequestParam("part_size") @PartSize partSize: Int,
        @AuthenticationPrincipal userId: Long
    ): List<ChannelDto> {
        return channelService.getSubscriptions(
            userId = userId,
            partIndex = partIndex,
            partSize = partSize
        ).map { it.toDto() }
    }

    @PatchMapping("/{channel_id}/subscription")
    fun subscribe(
        @PathVariable("channel_id") @LongId channelId: Long,
        @RequestParam("subscription") subscription: Subscription,
        @RequestParam("fcm_token") @NotBlank fcmToken: String,
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
        @RequestParam("fcm_token") @NotBlank token: String,
        @AuthenticationPrincipal userId: Long
    ) {
        channelService.subscribeToNotifications(userId = userId, token = token)
    }

    @DeleteMapping("/notifications/subscription")
    fun unsubscribeFromFCM(
        @RequestParam("fcm_token") @NotBlank token: String,
        @AuthenticationPrincipal userId: Long
    ) {
        channelService.unsubscribeFromNotifications(userId = userId, token = token)
    }

    @DeleteMapping("/{channel_id}")
    fun remove(
        @PathVariable("channel_id") @LongId channelId: Long,
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
    @field:LongId val channelId: Long,
    @field:Title val title: String,
    @field:Title val alias: String?,
    val editLogoAction: EditAction,
    val editHeaderAction: EditAction,
    @field:Description
    val description: String?
)
