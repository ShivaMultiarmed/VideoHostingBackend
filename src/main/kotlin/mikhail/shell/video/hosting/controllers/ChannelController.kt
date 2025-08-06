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
        @RequestParam userId: Long,
        @PathVariable channelId: Long
    ): ResponseEntity<ChannelWithUserDto> {
        val channelForUser = channelService.provideChannelForUser(channelId, userId)
        val channelForUserDto = channelForUser.toDto()
        return ResponseEntity.status(HttpStatus.OK).body(channelForUserDto)
    }

    @GetMapping("/{channelId}/cover")
    fun provideChannelCover(
        @PathVariable channelId: Long
    ): ResponseEntity<Resource> {
        return try {
            val coverFolder = java.io.File(CHANNEL_COVERS_BASE_PATH)
            val image = findFileByName(coverFolder, channelId.toString())
            if (image?.exists() != true || !channelService.checkExistsence(channelId)) {
                ResponseEntity.status(HttpStatus.NOT_FOUND).build()
            } else {
                val coverResource = FileSystemResource(image)
                ResponseEntity.status(HttpStatus.OK)
                    .contentType(MediaType.parseMediaType("image/${image.name.parseExtension()}"))
                    .body(coverResource)
            }
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/{channelId}/avatar")
    fun provideChannelAvatar(
        @PathVariable channelId: Long
    ): ResponseEntity<Resource> {
        return try {
            val avatarFolder = java.io.File(CHANNEL_AVATARS_BASE_PATH)
            val image = findFileByName(avatarFolder, channelId.toString())
            if (image?.exists() != true || !channelService.checkExistsence(channelId)) {
                ResponseEntity.status(HttpStatus.NOT_FOUND).build()
            } else {
                val avatarResource = FileSystemResource(image)
                ResponseEntity.status(HttpStatus.OK)
                    .contentType(MediaType.parseMediaType("image/${image.name.parseExtension()}"))
                    .body(avatarResource)
            }
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
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
        }
        avatarFile?.let {
            if (!it.contentType!!.contains("image")) {
                compoundError.add(AVATAR_TYPE_NOT_VALID)
            }
        }
        if (compoundError.isNotNull()) {
            throw ValidationException(compoundError)
        }
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
        if (channel.channelId == null || !channelService.checkExistsence(channel.channelId)) {
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
        }
        avatarFile?.let {
            if (!it.contentType!!.contains("image")) {
                compoundError.add(EditChannelError.AVATAR_TYPE_NOT_VALID)
            }
        }
        if (compoundError.isNotNull()) {
            throw ValidationException(compoundError)
        }
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
        val editedChannelDto = editedChannel.toDto()
        return ResponseEntity.status(HttpStatus.OK).body(editedChannelDto)
    }

    @GetMapping("/owner/{userId}")
    fun getAllChannelsByOwnerId(
        @PathVariable userId: Long
    ): ResponseEntity<List<ChannelDto>> {
        val channels = channelService.getChannelsByOwnerId(userId)
        val channelDtos = channels.map { it.toDto() }
        return ResponseEntity.status(HttpStatus.OK).body(channelDtos)
    }

    @GetMapping("/subscriber/{userId}")
    fun getAllChannelsBySubscriberId(
        @PathVariable userId: Long
    ): ResponseEntity<List<ChannelDto>> {
        val channels = channelService.getChannelsBySubscriberId(userId)
        val channelDtos = channels.map { it.toDto() }
        return ResponseEntity.status(HttpStatus.OK).body(channelDtos)
    }

    @PatchMapping("/{channelId}/subscribe")
    fun subscribeToChannel(
        @RequestParam userId: Long,
        @PathVariable channelId: Long,
        @RequestParam token: String,
        @RequestParam subscriptionState: SubscriptionState
    ): ResponseEntity<ChannelWithUserDto> {
        val realUserId = SecurityContextHolder.getContext().authentication.principal as Long?
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        if (realUserId != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        val channelWithUser = channelService.changeSubscriptionState(userId, channelId, token, subscriptionState)
        val channelWithUserDto = channelWithUser.toDto()
        return ResponseEntity.status(HttpStatus.OK).body(channelWithUserDto)
    }

    @PatchMapping("/notifications/subscribe")
    fun resubscribeToFCM(
        @RequestParam userId: Long,
        @RequestParam token: String
    ): ResponseEntity<Unit> {
        val realUserId = SecurityContextHolder.getContext().authentication.principal as Long?
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        if (realUserId != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        channelService.subscribeToNotifications(userId, token)
        return ResponseEntity.status(HttpStatus.OK).build()
    }

    @PatchMapping("/notifications/unsubscribe")
    fun unsubscribeFromFCM(
        @RequestParam userId: Long,
        @RequestParam token: String
    ): ResponseEntity<Unit> {
        val realUserId = SecurityContextHolder.getContext().authentication.principal as Long?
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        if (realUserId != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        channelService.unsubscribeFromNotifications(userId, token)
        return ResponseEntity.status(HttpStatus.OK).build()
    }

    @DeleteMapping("/{channelId}")
    fun removeChannel(
        @PathVariable channelId: Long
    ): ResponseEntity<Unit> {
        if (!channelService.checkExistsence(channelId)) {
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
        avatarUrl = "https://${constructReferenceBaseApiUrl(HOST)}/channels/$channelId/avatar",
        coverUrl = "https://${constructReferenceBaseApiUrl(HOST)}/channels/$channelId/cover"
    )

    private fun ChannelWithUser.toDto() = toDto(
        avatarUrl = "https://${constructReferenceBaseApiUrl(HOST)}/channels/$channelId/avatar",
        coverUrl = "https://${constructReferenceBaseApiUrl(HOST)}/channels/$channelId/cover"
    )
}
