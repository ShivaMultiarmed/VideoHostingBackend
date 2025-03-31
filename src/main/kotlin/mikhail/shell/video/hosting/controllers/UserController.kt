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
import mikhail.shell.video.hosting.errors.HostingDataException
import mikhail.shell.video.hosting.security.JwtTokenUtil
import mikhail.shell.video.hosting.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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
        request: HttpServletRequest,
        @PathVariable userId: Long
    ): ResponseEntity<UserDto> {
        val port = request.localPort
        val user = userService.get(userId)
        val userDto = userToDto(user, port)
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
        val port = request.localPort
        val token = request.getHeader("Authorization")?.substring(7)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val invokerId = jwtTokenUtil.extractUserId(token)
        if (userDto.userId != invokerId) {
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
            throw HostingDataException(compoundError)
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
        val editedUserDto = userToDto(editedUser, port)
        return ResponseEntity.ok(editedUserDto)
    }

    @DeleteMapping("/{userId}")
    fun remove(
        request: HttpServletRequest,
        @PathVariable userId: Long
    ): ResponseEntity<Unit> {
        val token = request.getHeader("Authorization")?.substring(7)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val invokerId = jwtTokenUtil.extractUserId(token)
        if (userId != invokerId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        userService.remove(userId)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/{userId}/avatar")
    fun getAvatar(@PathVariable userId: Long): ResponseEntity<ByteArray> {
        val bytes = userService.getAvatar(userId)
        return ResponseEntity.ok(bytes)
    }

    private fun userToDto(user: User, port: Int) = user.toDto(
        avatar = "https://$HOST:$port/api/v1/users/${user.userId}/avatar"
    )
}