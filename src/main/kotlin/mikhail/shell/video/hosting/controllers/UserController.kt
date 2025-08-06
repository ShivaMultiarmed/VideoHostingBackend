package mikhail.shell.video.hosting.controllers

import jakarta.servlet.http.HttpServletRequest
import mikhail.shell.video.hosting.domain.EditAction
import mikhail.shell.video.hosting.domain.File
import mikhail.shell.video.hosting.domain.User
import mikhail.shell.video.hosting.dto.UserDto
import mikhail.shell.video.hosting.dto.toDomain
import mikhail.shell.video.hosting.dto.toDto
import mikhail.shell.video.hosting.errors.CompoundError
import mikhail.shell.video.hosting.errors.EditUserError
import mikhail.shell.video.hosting.errors.ValidationException
import mikhail.shell.video.hosting.security.JwtTokenUtil
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

@RestController
@RequestMapping("/api/v1/users")
class UserController @Autowired constructor(
    private val userService: UserService,
    private val jwtTokenUtil: JwtTokenUtil
) {
    @Value("\${hosting.server.host}")
    private lateinit var HOST: String
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
        if (compoundError.isNotNull()) {
            throw ValidationException(compoundError)
        }
        val user = userDto.toDomain()
        val avatarFile = avatar?.let {
            File(
                name = it.originalFilename,
                mimeType = it.contentType,
                content = it.bytes
            )
        }
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
    fun getAvatar(@PathVariable userId: Long): ResponseEntity<Resource> {
        val avatarFile = userService.getAvatar(userId)
        val avatarResource = FileSystemResource(avatarFile)
        return ResponseEntity.ok(avatarResource)
    }

    private fun User.toDto() = toDto(
        avatar = "https://${constructReferenceBaseApiUrl(HOST)}/users/${userId}/avatar"
    )
}