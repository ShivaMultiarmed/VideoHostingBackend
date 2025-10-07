package mikhail.shell.video.hosting.controllers

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern
import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.domain.ValidationRules.TEL_REGEX
import mikhail.shell.video.hosting.dto.UserDto
import mikhail.shell.video.hosting.dto.toDto
import mikhail.shell.video.hosting.service.UserService
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
@RequestMapping("/api/v2/users")
class UserController @Autowired constructor(
    private val userService: UserService
) {
    @Value("\${video-hosting.server.base-url}")
    private lateinit var BASE_URL: String

    @GetMapping("/{user_id}")
    fun get(@PathVariable("user_id") @LongId userId: Long): UserDto {
        return userService.get(userId).toDto()
    }

    @GetMapping("/existence")
    fun exists(
        @RequestParam("purpose") @ValidEnum(NickCheckPurpose::class) purpose: String?,
        @RequestParam("nick") @Nick nick: String?,
        @AuthenticationPrincipal userId: Long?
    ): ResponseEntity<Unit> {
        return when(NickCheckPurpose.valueOf(purpose!!.uppercase())) {
            NickCheckPurpose.SIGN_UP -> {
                if (!userService.existsByNick(nick = nick!!)) {
                    ResponseEntity.status(HttpStatus.OK).build()
                } else {
                    ResponseEntity.status(HttpStatus.CONFLICT).build()
                }
            }
            NickCheckPurpose.EDIT -> {
                if (userId == null) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
                }
                if (userService.existsByNick(userId, nick!!) || !userService.existsByNick(null, nick)) {
                    ResponseEntity.status(HttpStatus.OK).build()
                } else {
                    ResponseEntity.status(HttpStatus.CONFLICT).build()
                }
            }
        }
    }

    @PatchMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun edit(
        @RequestPart("user") @Valid user: UserEditingRequest,
        @RequestPart("avatar") @Image avatar: MultipartFile?,
        @AuthenticationPrincipal userId: Long
    ): UserDto {
        return userService.edit(
            user = User(
                userId = userId,
                nick = user.nick!!,
                name = user.name,
                bio = user.bio,
                tel = user.tel,
                email = user.email
            ),
            avatarAction = EditAction.valueOf(user.avatarAction!!.uppercase()),
            avatar = avatar?.toUploadedFile()
        ).toDto()
    }

    @DeleteMapping
    fun remove(@AuthenticationPrincipal userId: Long) {
        userService.remove(userId)
    }

    @GetMapping("/{user_id}/avatar")
    fun getAvatar(
        @PathVariable("user_id") @LongId userId: Long,
        @RequestParam("size") @ValidEnum(ImageSize::class) size: String?
    ): ResponseEntity<Resource> {
        val image = userService.getAvatar(userId, ImageSize.valueOf(size!!.uppercase()))
        return ResponseEntity.status(HttpStatus.OK)
            .contentType(MediaType.parseMediaType("image/${image.file.extension}"))
            .body(image)
    }
}

data class UserCreatingRequest(
    @field:Name
    val nick: String,
    @field:Name
    val name: String?,
    @field:Description
    val bio: String?,
    @field:Pattern(regexp = TEL_REGEX, message = "PATTERN")
    val tel: String?,
    @field:Email(message = "PATTERN")
    val email: String?
)

data class UserEditingRequest(
    @field:Nick
    val nick: String?,
    @field:Name
    val name: String?,
    @field:Description
    val bio: String?,
    @field:Pattern(regexp = TEL_REGEX, message = "PATTERN")
    val tel: String?,
    @field:Email(message = "PATTERN")
    val email: String?,
    @field:ValidEnum(enumClass = EditAction::class)
    val avatarAction: String?
)