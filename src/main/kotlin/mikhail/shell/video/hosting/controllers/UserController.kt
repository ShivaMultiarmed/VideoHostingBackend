package mikhail.shell.video.hosting.controllers

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import mikhail.shell.video.hosting.domain.*
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

    @PatchMapping(consumes = ["multipart/form-data"])
    fun edit(
        @Validated @ModelAttribute user: UserEditingRequest,
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
            avatar = user.avatar?.toUploadedFile()
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

data class UserEditingRequest(
    @field:NotBlank @field:Max(ValidationRules.MAX_USERNAME_LENGTH.toLong())
    val nick: String,
    @field:NotBlank @field:Max(ValidationRules.MAX_NAME_LENGTH.toLong())
    val name: String?,
    @field:NotBlank @field:Max(ValidationRules.MAX_TEXT_LENGTH.toLong())
    val bio: String?,
    @field:Pattern(regexp = "^\\d{8,15}$")
    val tel: String?,
    @field:Email
    val email: String?,
    val avatarAction: EditAction,
    @field:FileValidation(
        max = ValidationRules.MAX_IMAGE_SIZE.toLong(),
        mime = "image"
    )
    val avatar: MultipartFile?
)