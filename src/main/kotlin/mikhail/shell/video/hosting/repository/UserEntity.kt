package mikhail.shell.video.hosting.repository

import jakarta.persistence.*
import mikhail.shell.video.hosting.domain.User

@Entity
@Table(name = "users")
data class UserEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    val userId: Long? = null,
    val name: String? = null,
    val nick: String,
    val bio: String? = null,
    val tel: String? = null,
    val email: String? = null
)

fun User.toEntity() = UserEntity(
    userId = userId,
    name = name,
    nick = nick,
    bio = bio,
    tel = tel,
    email = email
)
fun UserEntity.toDomain() = User(
    userId = userId,
    name = name,
    nick = nick,
    bio = bio,
    tel = tel,
    email = email
)

@Entity
@Table(name = "auth_details")
data class AuthDetailEntity(
    @EmbeddedId
    val id: AuthDetailEntityId,
    val username: String,
)

@Embeddable
data class AuthDetailEntityId(
    val userId: Long,
    @Enumerated(value = EnumType.STRING)
    val method: AuthenticationMethod
)

enum class AuthenticationMethod {
    EMAIL, TEL, GOOGLE, VK, TG
}

@Entity
@Table(name = "passwords")
data class PasswordEntity(
    val id: Long,
    val password: String
)