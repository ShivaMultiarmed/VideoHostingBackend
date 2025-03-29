package mikhail.shell.video.hosting.dto

import mikhail.shell.video.hosting.domain.User

data class UserDto(
    val userId: Long? = null,
    val name: String? = null,
    val nick: String,
    val avatar: String? = null,
    val bio: String? = null,
    val tel: Int? = null,
    val email: String? = null
)

fun User.toDto(avatar: String? = null) = UserDto(userId, name, nick, avatar, bio, tel, email)
fun UserDto.toDomain() = User(userId, name, nick, bio, tel, email)

data class SignUpDto(
    val userName: String? = null,
    val password: String? = null,
    val userDto: UserDto? = null
)
