package mikhail.shell.video.hosting.controllers

import jakarta.servlet.http.HttpServletRequest
import mikhail.shell.video.hosting.domain.EditAction
import mikhail.shell.video.hosting.domain.File
import mikhail.shell.video.hosting.dto.UserDto
import mikhail.shell.video.hosting.dto.toDomain
import mikhail.shell.video.hosting.dto.toDto
import mikhail.shell.video.hosting.errors.CompoundError
import mikhail.shell.video.hosting.errors.EditUserError
import mikhail.shell.video.hosting.errors.HostingDataException
import mikhail.shell.video.hosting.security.JwtTokenUtil
import mikhail.shell.video.hosting.service.UserService
import org.springframework.beans.factory.annotation.Autowired
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
    @GetMapping("/{userId}")
    fun get(@PathVariable userId: Long): ResponseEntity<UserDto> {
        val user = userService.get(userId)
        val userDto = user.toDto(
            avatar = null // TODO
        )
        return ResponseEntity.ok(userDto)
    }

    @PatchMapping(
        "/edit",
        consumes = ["multipart/form-data"]
    )
    fun edit(
        request: HttpServletRequest,
        @RequestPart userDto: UserDto,
        @RequestPart avatarAction: EditAction,
        @RequestPart avatar: MultipartFile
    ): ResponseEntity<UserDto> {
        val token = request.getHeader("Authorization")?.substring(7)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val invokerId = jwtTokenUtil.extractUserId(token)
        if (userDto.userId != invokerId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        val compoundError = CompoundError<EditUserError>()
        if (userDto.userId == null) {
            throw NoSuchElementException()
        }
        if (userDto.nick.isEmpty()) {
            compoundError.add(EditUserError.NICK_EMPTY)
        }
        if (userDto.age?.toInt() !in 0..127) {
            compoundError.add(EditUserError.AGE_MALFORMED)
        }
        val telRegex = Regex("^\\+\\d{11,15}$")
        userDto.tel?.let {
            if (!telRegex.matches(it.toString())) {
                compoundError.add(EditUserError.TEL_MALFORMED)
            }
        }
        val emailRegex = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\\\.[a-zA-Z]{2,}\$")
        userDto.email?.let {
            if (!emailRegex.matches(it)) {
                compoundError.add(EditUserError.EMAIL_MALFORMED)
            }
        }
        if (compoundError.isNotNull()) {
            throw HostingDataException(compoundError)
        }
        val user = userDto.toDomain()
        val avatarFile = File(
            name = avatar.originalFilename,
            mimeType = avatar.contentType,
            content = avatar.bytes
        )
        val editedUser = userService.edit(user, avatarAction, avatarFile)
        val editedUserDto = editedUser.toDto()
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
}