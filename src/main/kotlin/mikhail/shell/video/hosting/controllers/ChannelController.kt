package mikhail.shell.video.hosting.controllers

import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.domain.ApplicationPaths.CHANNEL_AVATARS_BASE_PATH
import mikhail.shell.video.hosting.domain.ApplicationPaths.CHANNEL_COVERS_BASE_PATH
import mikhail.shell.video.hosting.dto.ChannelDto
import mikhail.shell.video.hosting.dto.ChannelWithUserDto
import mikhail.shell.video.hosting.dto.toDomain
import mikhail.shell.video.hosting.dto.toDto
import mikhail.shell.video.hosting.errors.ChannelCreationError
import mikhail.shell.video.hosting.errors.ChannelCreationError.*
import mikhail.shell.video.hosting.errors.CompoundError
import mikhail.shell.video.hosting.errors.EditChannelError
import mikhail.shell.video.hosting.errors.ValidationException
import mikhail.shell.video.hosting.service.ChannelService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Paths

@RestController
@RequestMapping("/api/v1/channels")
class ChannelController @Autowired constructor(
    private val channelService: ChannelService
) {
    @Value("\${hosting.server.host}")
    private lateinit var HOST: String

    @GetMapping("/{channelId}")
    fun provideChannel(
        @PathVariable channelId: Long
    ): ResponseEntity<ChannelDto> {
        val channel = channelService.getChannel(channelId)
        val channelDto = channel.toDto()
        return ResponseEntity.status(HttpStatus.OK).body(channelDto)
    }

    @GetMapping("/{channelId}/details")
    fun provideChannelDetails(
        @PathVariable channelId: Long
    ): ResponseEntity<ChannelWithUserDto> {
        val userId = SecurityContextHolder.getContext().authentication.principal as Long?
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val channelForUser = channelService.provideChannelForUser(channelId, userId)
        val channelForUserDto = channelForUser.toDto()
        return ResponseEntity.status(HttpStatus.OK).body(channelForUserDto)
    }

    @GetMapping("/{channelId}/cover")
    fun provideChannelCover(@PathVariable channelId: Long): ResponseEntity<Resource> {
        val coverFolder = Paths.get(CHANNEL_COVERS_BASE_PATH).toFile()
        val image = findFileByName(coverFolder, channelId.toString())
        return if (image?.exists() != true || !channelService.checkExistence(channelId)) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        } else {
            val coverResource = FileSystemResource(image)
            ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.parseMediaType("image/${image.name.parseExtension()}"))
                .body(coverResource)
        }
    }

    @GetMapping("/{channelId}/avatar")
    fun provideChannelAvatar(@PathVariable channelId: Long): ResponseEntity<Resource> {
        val avatarFolder = Paths.get(CHANNEL_AVATARS_BASE_PATH).toFile()
        val image = findFileByName(avatarFolder, channelId.toString())
        return if (image?.exists() != true || !channelService.checkExistence(channelId)) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        } else {
            val avatarResource = FileSystemResource(image)
            ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.parseMediaType("image/${image.name.parseExtension()}"))
                .body(avatarResource)
        }
    }

    @PostMapping(
        path = ["/create"],
        consumes = ["multipart/form-data"]
    )
    fun createChannel(
        @RequestPart("channel") channel: ChannelDto,
        @RequestPart("cover") coverFile: MultipartFile?,
        @RequestPart("avatar") avatarFile: MultipartFile?
    ): ResponseEntity<ChannelDto> {
        val userId = SecurityContextHolder.getContext().authentication.principal as Long?
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        if (userId != channel.ownerId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        val compoundError = CompoundError<ChannelCreationError>()
        if (channel.title.isEmpty()) {
            compoundError.add(TITLE_EMPTY)
        }
        coverFile?.let {
            if (!it.contentType!!.contains("image")) {
                compoundError.add(COVER_TYPE_NOT_VALID)
            }
            if (it.size > ValidationRules.MAX_IMAGE_SIZE) {
                compoundError.add(COVER_TOO_LARGE)
            }
        }
        avatarFile?.let {
            if (!it.contentType!!.contains("image")) {
                compoundError.add(AVATAR_TYPE_NOT_VALID)
            }
            if (it.size > ValidationRules.MAX_IMAGE_SIZE) {
                compoundError.add(AVATAR_TOO_LARGE)
            }
        }

        if (compoundError.isNotEmpty()) {
            throw ValidationException(compoundError)
        }

        val cover = coverFile?.toUploadedFile()
        val avatar = avatarFile?.toUploadedFile()
        val createdChannel = channelService.createChannel(
            channel = channel
                .toDomain()
                .copy(channelId = null),
            cover = cover,
            avatar = avatar
        )
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(createdChannel.toDto())
    }

    @PatchMapping(
        path = ["/edit"],
        consumes = ["multipart/form-data"]
    )
    fun editChannel(
        @RequestPart("channel") channel: ChannelDto,
        @RequestPart("editCoverAction") editCoverAction: EditAction,
        @RequestPart("cover") coverFile: MultipartFile?,
        @RequestPart("editAvatarAction") editAvatarAction: EditAction,
        @RequestPart("avatar") avatarFile: MultipartFile?
    ): ResponseEntity<ChannelDto> {
        val userId = SecurityContextHolder.getContext().authentication.principal as Long?
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        if (channel.channelId == null || !channelService.checkExistence(channel.channelId)) {
            throw NoSuchElementException()
        }
        if (!channelService.checkOwner(userId, channel.channelId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        val compoundError = CompoundError<EditChannelError>()
        if (channel.title.isEmpty()) {
            compoundError.add(EditChannelError.TITLE_EMPTY)
        }
        coverFile?.let {
            if (!it.contentType!!.contains("image")) {
                compoundError.add(EditChannelError.COVER_TYPE_NOT_VALID)
            }
            if (it.size > ValidationRules.MAX_IMAGE_SIZE) {
                compoundError.add(EditChannelError.COVER_TOO_LARGE)
            }
        }
        avatarFile?.let {
            if (!it.contentType!!.contains("image")) {
                compoundError.add(EditChannelError.AVATAR_TYPE_NOT_VALID)
            }
            if (it.size > ValidationRules.MAX_IMAGE_SIZE) {
                compoundError.add(EditChannelError.AVATAR_TOO_LARGE)
            }
        }
        if (compoundError.isNotEmpty()) {
            throw ValidationException(compoundError)
        }
        val editedChannel = channelService.editChannel(
            channel = channel.toDomain(),
            editCoverAction = editCoverAction,
            coverFile = coverFile?.toUploadedFile(),
            editAvatarAction = editAvatarAction,
            avatarFile = avatarFile?.toUploadedFile()
        )
        val editedChannelDto = editedChannel.toDto()
        return ResponseEntity.status(HttpStatus.OK).body(editedChannelDto)
    }

    @GetMapping("/owner/{userId}")
    fun getAllChannelsByOwnerId(@PathVariable userId: Long): ResponseEntity<List<ChannelDto>> {
        val channels = channelService.getChannelsByOwnerId(userId)
        val channelDtos = channels.map { it.toDto() }
        return ResponseEntity.status(HttpStatus.OK).body(channelDtos)
    }

    @GetMapping("/subscriptions")
    fun getAllChannelsBySubscriberId(): ResponseEntity<List<ChannelDto>> {
        val userId = SecurityContextHolder.getContext().authentication.principal as Long?
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val channels = channelService.getChannelsBySubscriberId(userId)
        val channelDtos = channels.map { it.toDto() }
        return ResponseEntity.status(HttpStatus.OK).body(channelDtos)
    }

    @PatchMapping("/{channelId}/subscribe")
    fun subscribeToChannel(@PathVariable channelId: Long, @RequestParam fcmToken: String): ResponseEntity<Unit> {
        val userId = SecurityContextHolder.getContext().authentication.principal as Long?
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        channelService.changeSubscriptionState(userId, channelId, fcmToken)
        return ResponseEntity.status(HttpStatus.OK).build()
    }

    @PatchMapping("/notifications/subscribe")
    fun resubscribeToFCM(
        @RequestParam token: String
    ): ResponseEntity<Unit> {
        val userId = SecurityContextHolder.getContext().authentication.principal as Long?
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        channelService.subscribeToNotifications(userId, token)
        return ResponseEntity.status(HttpStatus.OK).build()
    }

    @PatchMapping("/notifications/unsubscribe")
    fun unsubscribeFromFCM(
        @RequestParam token: String
    ): ResponseEntity<Unit> {
        val userId = SecurityContextHolder.getContext().authentication.principal as Long?
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        channelService.unsubscribeFromNotifications(userId, token)
        return ResponseEntity.status(HttpStatus.OK).build()
    }

    @DeleteMapping("/{channelId}")
    fun removeChannel(
        @PathVariable channelId: Long
    ): ResponseEntity<Unit> {
        if (!channelService.checkExistence(channelId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
        val userId = SecurityContextHolder.getContext().authentication.principal as Long?
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        if (channelService.checkOwner(userId, channelId)) {
            channelService.removeChannel(channelId)
            return ResponseEntity.status(HttpStatus.OK).build()
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
    }

    private fun Channel.toDto(): ChannelDto = toDto(
        avatarUrl = "https://${constructReferenceBaseApiUrl(HOST)}/channels/$channelId/avatar"
    )

    private fun ChannelWithUser.toDto() = toDto(
        avatarUrl = "https://${constructReferenceBaseApiUrl(HOST)}/channels/$channelId/avatar",
        coverUrl = "https://${constructReferenceBaseApiUrl(HOST)}/channels/$channelId/cover"
    )
}
