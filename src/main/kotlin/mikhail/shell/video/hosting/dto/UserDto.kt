package mikhail.shell.video.hosting.dto

import mikhail.shell.video.hosting.domain.User

data class UserDto(
    val userId: Long?,
    val name: String
)

fun User.toDto() = UserDto(userId, name)
fun UserDto.toDomain() = User(userId, name)

data class SignUpDto(
    val userName: String,
    val password: String,
    val userDto: UserDto
)
