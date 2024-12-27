package mikhail.shell.video.hosting.dto

import mikhail.shell.video.hosting.domain.User

data class UserDto(
    val userId: Long?,
    val name: String?
)

fun User.toDto() = UserDto(userId, name)
fun UserDto?.toDomain(): User? {
    return if (this != null) User(userId, name) else null
}

data class SignUpDto(
    val userName: String? = null,
    val password: String? = null,
    val userDto: UserDto? = null
)
