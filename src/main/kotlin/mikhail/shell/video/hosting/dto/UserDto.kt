package mikhail.shell.video.hosting.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import mikhail.shell.video.hosting.controllers.UserCreatingRequest
import mikhail.shell.video.hosting.domain.User
import mikhail.shell.video.hosting.domain.ValidationRules.PASSWORD_REGEX

data class UserDto(
    val userId: Long,
    val nick: String,
    val name: String?,
    val bio: String?,
    val tel: String?,
    val email: String?
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
