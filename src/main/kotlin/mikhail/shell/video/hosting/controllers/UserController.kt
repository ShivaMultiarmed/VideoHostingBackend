package mikhail.shell.video.hosting.controllers

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.domain.ValidationRules.MAX_IMAGE_SIZE
import mikhail.shell.video.hosting.domain.ValidationRules.MAX_NAME_LENGTH
import mikhail.shell.video.hosting.domain.ValidationRules.MAX_TEXT_LENGTH
import mikhail.shell.video.hosting.domain.ValidationRules.MAX_USERNAME_LENGTH
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
    fun get(@PathVariable userId: Long): UserDto {
        return userService.get(userId).toDto()
    }

    @PatchMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun edit(
        @RequestPart @Valid user: UserEditingRequest,
        @RequestPart @FileSize(MAX_IMAGE_SIZE) @FileType("image")  avatar: MultipartFile?,
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
    fun getAvatar(@PathVariable @Positive userId: Long): ResponseEntity<Resource> {
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
    @field:NotBlank @field:Size(max = MAX_USERNAME_LENGTH, message = "LARGE")
    val nick: String,
    @field:NotBlank @field:Size(max = MAX_NAME_LENGTH, message = "LARGE")
    val name: String?,
    @field:NotBlank @field:Size(max = MAX_TEXT_LENGTH, message = "LARGE")
    val bio: String?,
    @field:Pattern(regexp = "^\\d{8,15}$", message = "PATTERN")
    val tel: String?,
    @field:Email(message = "PATTERN")
    val email: String?
)

data class UserEditingRequest(
    @field:NotBlank @field:Size(max = MAX_USERNAME_LENGTH, message = "LARGE")
    val nick: String,
    @field:NotBlank @field:Size(max = MAX_NAME_LENGTH, message = "LARGE")
    val name: String?,
    @field:NotBlank @field:Size(max = MAX_TEXT_LENGTH, message = "LARGE")
    val bio: String?,
    @field:Pattern(regexp = "^\\d{8,15}$", message = "PATTERN")
    val tel: String?,
    @field:Email(message = "PATTERN")
    val email: String?,
    val avatarAction: EditAction,
)