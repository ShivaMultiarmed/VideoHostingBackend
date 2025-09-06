package mikhail.shell.video.hosting.controllers

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
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
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v2/users")
class UserController @Autowired constructor(
    private val userService: UserService
) {
    @Value("\${video-hosting.server.base-url}")
    private lateinit var BASE_URL: String
    @GetMapping("/{userId}")
    fun get(@PathVariable @Positive(message = "LOW") userId: Long): UserDto {
        return userService.get(userId).toDto()
    }

    @PatchMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun edit(
        @RequestPart @Valid user: UserEditingRequest,
        @RequestPart @Image avatar: MultipartFile?,
        @AuthenticationPrincipal userId: Long
    ): UserDto {
        return userService.edit(
            user = User(
                userId = userId,
                name = user.name,
                nick = user.nick,
                bio = user.bio,
                tel = user.tel,
                email = user.email
            ),
            avatarAction = user.avatarAction,
            avatar = avatar?.toUploadedFile()
        ).toDto()
    }

    @DeleteMapping
    fun remove(@AuthenticationPrincipal userId: Long) {
        userService.remove(userId)
    }

    @GetMapping("/{userId}/avatar")
    fun getAvatar(@PathVariable @LongId userId: Long): ResponseEntity<Resource> {
        val image = userService.getAvatar(userId)
        return ResponseEntity.status(HttpStatus.OK)
            .contentType(MediaType.parseMediaType("image/${image.file.extension}"))
            .body(image)
    }

    private fun User.toDto() = toDto(
        avatar = "$BASE_URL/users/$userId/avatar"
    )
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
    @field:Name
    val nick: String,
    @field:Name
    val name: String?,
    @field:Description
    val bio: String?,
    @field:Pattern(regexp = TEL_REGEX, message = "PATTERN")
    val tel: String?,
    @field:Email(message = "PATTERN")
    val email: String?,
    val avatarAction: EditAction,
)