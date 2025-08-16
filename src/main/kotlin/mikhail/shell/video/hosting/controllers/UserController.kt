package mikhail.shell.video.hosting.controllers

import jakarta.servlet.http.HttpServletRequest
import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.domain.ApplicationPaths.USER_AVATARS_BASE_PATH
import mikhail.shell.video.hosting.dto.UserDto
import mikhail.shell.video.hosting.dto.toDomain
import mikhail.shell.video.hosting.dto.toDto
import mikhail.shell.video.hosting.errors.CompoundError
import mikhail.shell.video.hosting.errors.EditUserError
import mikhail.shell.video.hosting.errors.ValidationException
import mikhail.shell.video.hosting.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Paths

@RestController
@RequestMapping("/api/v1/users")
class UserController @Autowired constructor(
    private val userService: UserService
) {
    @Value("\${video-hosting.server.base-url}")
    private lateinit var BASE_URL: String
    @GetMapping("/{userId}")
    fun get(
        @PathVariable userId: Long
    ): ResponseEntity<UserDto> {
        val user = userService.get(userId)
        val userDto = user.toDto()
        return ResponseEntity.ok(userDto)
    }

    @PatchMapping(
        "/edit",
        consumes = ["multipart/form-data"]
    )
    fun edit(
        request: HttpServletRequest,
        @RequestPart(name = "user") userDto: UserDto,
        @RequestPart avatarAction: EditAction,
        @RequestPart avatar: MultipartFile?
    ): ResponseEntity<UserDto> {
        if (!userService.checkExistence(userDto.userId?: 0)) {
            throw NoSuchElementException()
        }
        val userId = SecurityContextHolder.getContext().authentication.principal as Long?
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        if (userDto.userId != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        val compoundError = CompoundError<EditUserError>()
        if (userDto.nick.isEmpty()) {
            compoundError.add(EditUserError.NICK_EMPTY)
        }
        val telRegex = Regex("^\\d{11,15}$")
        if (userDto.tel?.let { !telRegex.matches(it) } == true) {
            compoundError.add(EditUserError.TEL_MALFORMED)
        }
        val emailRegex = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$")
        if (userDto.email?.let { !emailRegex.matches(it) } == true) {
            compoundError.add(EditUserError.EMAIL_MALFORMED)
        }
        if ((avatar?.size?: 0) > ValidationRules.MAX_IMAGE_SIZE) {
            compoundError.add(EditUserError.AVATAR_TOO_LARGE)
        }
        if (compoundError.isNotEmpty()) {
            throw ValidationException(compoundError)
        }
        val user = userDto.toDomain()
        val avatarFile = avatar?.toUploadedFile()
        val editedUser = userService.edit(user, avatarAction, avatarFile)
        val editedUserDto = editedUser.toDto()
        return ResponseEntity.ok(editedUserDto)
    }

    @DeleteMapping
    fun remove(): ResponseEntity<Unit> {
        val userId = SecurityContextHolder.getContext().authentication.principal as Long?
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        userService.remove(userId)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/{userId}/avatar")
    fun getAvatar(
        @PathVariable userId: Long,
        @PathVariable size: String
    ): ResponseEntity<Resource> {
        val avatarFolder = Paths.get(USER_AVATARS_BASE_PATH).toFile()
        val avatarFile = findFileByName(avatarFolder, userId.toString())
        if (avatarFile == null || !userService.checkExistence(userId)) {
            throw NoSuchElementException()
        }
        val avatarResource = FileSystemResource(avatarFile)
        return ResponseEntity.ok(avatarResource)
    }

    private fun User.toDto() = toDto(
        avatar = "$BASE_URL/users/$userId/avatar"
    )
}