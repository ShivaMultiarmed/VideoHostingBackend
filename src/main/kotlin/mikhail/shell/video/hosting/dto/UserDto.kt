package mikhail.shell.video.hosting.dto

import mikhail.shell.video.hosting.domain.User

data class UserDto(
    val userId: Long? = null,
    val name: String? = null,
    val nick: String,
    val avatar: String? = null,
    val bio: String? = null,
    val tel: String? = null,
    val email: String? = null
)

fun User.toDto(avatar: String? = null) = UserDto(
    userId = userId,
    name = name,
    nick = nick,
    avatar = avatar,
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

data class SignUpDto(
    val password: String,
    val userDto: UserDto
)
