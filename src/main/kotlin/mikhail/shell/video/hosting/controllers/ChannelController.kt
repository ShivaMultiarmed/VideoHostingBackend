package mikhail.shell.video.hosting.controllers

import jakarta.validation.Valid
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
    fun get(@PathVariable("channel_id") @LongId channelId: Long?): ChannelDto {
        return channelService.getChannel(channelId!!).toDto()
    }

    @GetMapping("/{channel_id}/details")
    fun getDetails(
        @PathVariable("channel_id") @LongId channelId: Long?,
        @AuthenticationPrincipal userId: Long
    ): ChannelWithUserDto {
        return channelService.getForUser(channelId = channelId!!, userId = userId).toDto()
    }

    @GetMapping("/{channel_id}/header")
    fun getHeader(
        @PathVariable("channel_id") @LongId channelId: Long?,
        @RequestParam("size") @ValidEnum(ImageSize::class) size: String?
    ): ResponseEntity<Resource> {
        val image = channelService.getHeader(channelId!!, ImageSize.valueOf(size!!.uppercase()))
        return ResponseEntity.status(HttpStatus.OK)
            .contentType(MediaType.parseMediaType("image/${image.file.extension}"))
            .body(image)
    }

    @GetMapping("/{channel_id}/logo")
    fun getLogo(
        @PathVariable("channel_id") @LongId channelId: Long?,
        @RequestParam("size") @ValidEnum(ImageSize::class) size: String?
    ): ResponseEntity<Resource> {
        val image = channelService.getLogo(channelId!!, ImageSize.valueOf(size!!.uppercase()))
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.parseMediaType("image/${image.file.extension}"))
                .body(image)
    }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createChannel(
        @RequestPart("channel") @Valid channel: ChannelCreationRequest,
        @RequestPart("logo", required = false) @Image logo: MultipartFile?,
        @RequestPart("header", required = false) @Image header: MultipartFile?,
        @AuthenticationPrincipal userId: Long,
    ): ChannelDto {
        return channelService.createChannel(
            channel = ChannelCreationModel(
                ownerId = userId,
                title = channel.title!!,
                alias = channel.alias,
                description = channel.description,
                header = header?.toUploadedFile(),
                logo = logo?.toUploadedFile()
            )
        ).toDto()
    }

    @PutMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun editChannel(
        @RequestPart("channel") @Valid channel: ChannelEditingRequest,
        @RequestPart("logo", required = false) @Image logo: MultipartFile?,
        @RequestPart("header", required = false) @Image header: MultipartFile?,
        @AuthenticationPrincipal userId: Long
    ): ChannelDto {
        return channelService.editChannel(
            channel = ChannelEditingModel(
                channelId = channel.channelId!!,
                ownerId = userId,
                title = channel.title!!,
                alias = channel.alias,
                description = channel.description,
                header = when (EditAction.valueOf(channel.headerAction!!.uppercase())) {
                    EditAction.KEEP -> EditingAction.Keep
                    EditAction.REMOVE -> EditingAction.Remove
                    EditAction.EDIT -> EditingAction.Edit(header!!.toUploadedFile())
                },
                logo = when (EditAction.valueOf(channel.logoAction!!.uppercase())) {
                    EditAction.KEEP -> EditingAction.Keep
                    EditAction.REMOVE -> EditingAction.Remove
                    EditAction.EDIT -> EditingAction.Edit(logo!!.toUploadedFile())
                }
            )
        ).toDto()
    }

    @GetMapping("/owner/{user_id}")
    fun getOwnedChannels(
        @PathVariable("user_id") @LongId userId: Long?,
        @RequestParam("part_index") @PartIndex partIndex: Long = 0,
        @RequestParam("part_size") @PartSize partSize: Int = 10
    ): List<ChannelDto> {
        return channelService.getChannelsByOwnerId(
            userId = userId!!,
            partIndex = partIndex,
            partSize = partSize
        ).map { it.toDto() }
    }

    @GetMapping("/subscriptions")
    fun getSubscriptions(
        @RequestParam("part_index") @PartIndex partIndex: Long = 0,
        @RequestParam("part_size") @PartSize partSize: Int = 10,
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
        @PathVariable("channel_id") @LongId channelId: Long?,
        @RequestParam("subscription") @ValidEnum(Subscription::class) subscription: String?,
        @RequestHeader("Messaging-Token") token: String?,
        @AuthenticationPrincipal userId: Long
    ): ChannelWithUserDto {
        return channelService.subscribe(
            subscriberId = userId,
            channelId = channelId!!,
            subscription = Subscription.valueOf(subscription!!.uppercase()),
            token = token
        ).toDto()
    }

    @DeleteMapping("/{channel_id}")
    fun remove(
        @PathVariable("channel_id") @LongId channelId: Long?,
        @AuthenticationPrincipal userId: Long
    ) {
        channelService.removeChannel(userId = userId, channelId = channelId!!)
    }

    @GetMapping("/existence")
    fun exists(
        @RequestParam("channel_id") @LongIdNullable channelId: Long?,
        @RequestParam("title", required = false) @TitleNullable title: String?,
        @RequestParam("alias", required = false) @Alias alias: String?
    ): ResponseEntity<Unit> {
        val params = mapOf("title" to title, "alias" to alias).filter { it.value != null }
        if (params.size != 1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }
        val key = params.entries.first().key
        return if (key == "title") {
            if (channelId != null && (channelService.existsByTitle(channelId, title!!) || !channelService.existsByTitle(null, title)) || channelId == null && !channelService.existsByTitle(null, title!!)) {
                ResponseEntity.status(HttpStatus.OK).build()
            } else {
                ResponseEntity.status(HttpStatus.CONFLICT).build()
            }
        } else {
            if (channelId != null && (channelService.existsByAlias(channelId, alias!!) || !channelService.existsByAlias(null, alias)) || channelId == null && !channelService.existsByAlias(null, alias!!)) {
                ResponseEntity.status(HttpStatus.OK).body(Unit)
            } else {
                ResponseEntity.status(HttpStatus.CONFLICT).body(Unit)
            }
        }
    }
}

data class ChannelCreationRequest(
    @field:Title
    val title: String?,
    @field:Alias
    val alias: String?,
    @field:Description
    val description: String?
)

data class ChannelEditingRequest(
    @field:LongId
    val channelId: Long?,
    @field:Title
    val title: String?,
    @field:Alias
    val alias: String?,
    @field:Description
    val description: String?,
    @field:ValidEnum(EditAction::class)
    val headerAction: String?,
    @field:ValidEnum(EditAction::class)
    val logoAction: String?
)
