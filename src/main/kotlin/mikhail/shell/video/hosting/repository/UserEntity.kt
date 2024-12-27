package mikhail.shell.video.hosting.repository

import jakarta.persistence.*
import mikhail.shell.video.hosting.domain.User

@Entity
@Table(name = "users")
data class UserEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val userId: Long?,
    val name: String
)

fun User.toEntity(): UserEntity {
    if (name == null)
        throw Exception()
    return UserEntity(userId, name)
}
fun UserEntity.toDomain() = User(userId,name)

@Entity
@Table(name = "credentials")
data class Credential(
    @EmbeddedId
    val id: CredentialId,
    val credential: String,
    val userName: String
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