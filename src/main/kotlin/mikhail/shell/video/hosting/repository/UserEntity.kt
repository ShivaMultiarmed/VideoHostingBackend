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
    val tel: Int? = null,
    val email: String? = null
)

fun User.toEntity() = UserEntity(userId, name, nick, bio, tel, email)
fun UserEntity.toDomain() = User(userId, name, nick, bio, tel, email)

@Entity
@Table(name = "credentials")
data class Credential(
    @EmbeddedId
    val id: CredentialId,
    val userName: String,
    val credential: String // TODO make it nullable (If authentication method is not PASSWORD)
)

@Embeddable
data class CredentialId(
    val userId: Long,
    @Enumerated(value = EnumType.STRING)
    val method: AuthenticationMethod
)

enum class AuthenticationMethod {
    PASSWORD, GOOGLE, VK
}