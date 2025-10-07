package mikhail.shell.video.hosting.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import mikhail.shell.video.hosting.controllers.UserCreatingRequest
import mikhail.shell.video.hosting.domain.User
import mikhail.shell.video.hosting.domain.ValidationRules.PASSWORD_REGEX

data class UserDto(
    val userId: Long? = null,
    val name: String? = null,
    val nick: String,
    val bio: String? = null,
    val tel: String? = null,
    val email: String? = null
)

fun User.toDto() = UserDto(
    userId = userId,
    name = name,
    nick = nick,
    bio = bio,
    tel = tel,
    email = email
)
fun UserDto.toDomain() = User(
    userId = userId,
    name = name,
    nick = nick,
    bio = bio,
    tel = tel,
    email = email
)

data class SignUpRequest(
    @field:NotBlank(message = "EMPTY") @field:Pattern(regexp = PASSWORD_REGEX, message = "PATTERN")
    val password: String,
    val userCreatingRequest: UserCreatingRequest
)
