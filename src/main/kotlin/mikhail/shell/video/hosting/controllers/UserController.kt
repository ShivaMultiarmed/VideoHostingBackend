package mikhail.shell.video.hosting.controllers

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern
import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.domain.ValidationRules.TEL_REGEX
import mikhail.shell.video.hosting.dto.EditingActionDto
import mikhail.shell.video.hosting.dto.UserDto
import mikhail.shell.video.hosting.dto.toDto
import mikhail.shell.video.hosting.dto.toUploadedFile
import mikhail.shell.video.hosting.errors.FileError
import mikhail.shell.video.hosting.errors.ValidationException
import mikhail.shell.video.hosting.service.AuthService
import mikhail.shell.video.hosting.service.UserService
import org.springframework.beans.factory.annotation.Autowired
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
    private val userService: UserService,
    private val authService: AuthService,
) {
    @GetMapping("/{user_id}")
    fun get(@PathVariable("user_id") @LongId userId: Long): UserDto {
        return userService.get(userId).toDto()
    }

    @GetMapping("/existence")
    fun exists(
        @RequestParam("purpose") purpose: NickCheckPurpose,
        @RequestParam("nick") @Nick nick: String?,
        @AuthenticationPrincipal userId: Long?
    ): ResponseEntity<Unit> {
        return when(purpose) {
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

    @PutMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun edit(
        @RequestPart("user") @Valid user: UserEditingRequest,
        @RequestPart("avatar") @Image avatar: MultipartFile?,
        @AuthenticationPrincipal userId: Long
    ): UserDto {
        val errors = mutableMapOf<String, FileError>()
        if (user.avatarAction == EditingActionDto.EDIT && avatar == null) {
            errors["avatar"] = FileError.EMPTY
        }
        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }
        return userService.edit(
            user = UserEditingModel(
                userId = userId,
                nick = user.nick!!,
                name = user.name,
                bio = user.bio,
                tel = user.tel,
                email = user.email,
                avatar = when (user.avatarAction) {
                    EditingActionDto.KEEP -> EditingAction.Keep
                    EditingActionDto.REMOVE -> EditingAction.Remove
                    EditingActionDto.EDIT -> EditingAction.Edit(avatar!!.toUploadedFile())
                }
            )
        ).toDto()
    }

    @DeleteMapping
    fun remove(
        @RequestHeader("Authorization") authorization: String,
        @AuthenticationPrincipal userId: Long
    ) {
        val token = authorization.removePrefix("Bearer ")
        authService.signOut(token)
        userService.remove(userId)
    }

    @GetMapping("/{user_id}/avatar")
    fun getAvatar(
        @PathVariable("user_id") @LongId userId: Long,
        @RequestParam("size") size: ImageSize
    ): ResponseEntity<Resource> {
        val image = userService.getAvatar(userId, size)
        return ResponseEntity.status(HttpStatus.OK)
            .contentType(MediaType.parseMediaType("image/${image.file.extension}"))
            .body(image)
    }

    @PostMapping("/notifications/subscription")
    fun subscribeToNotifications(
        @RequestHeader("Messaging-Token") token: String,
        @AuthenticationPrincipal userId: Long
    ) {
        userService.subscribeToNotifications(userId = userId, token = token)
    }

    @DeleteMapping("/notifications/subscription")
    fun unsubscribeFromNotifications(
        @RequestHeader("Messaging-Token") token: String,
        @AuthenticationPrincipal userId: Long
    ) {
        userService.unsubscribeFromNotifications(userId = userId, token = token)
    }
}

data class UserCreatingRequest(
    @field:Password
    val password: String?,
    @field:Nick
    val nick: String?
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
    val avatarAction: EditingActionDto = EditingActionDto.KEEP
)