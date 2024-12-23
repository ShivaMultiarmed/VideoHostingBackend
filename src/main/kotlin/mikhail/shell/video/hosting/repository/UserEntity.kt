package mikhail.shell.video.hosting.repository

import jakarta.persistence.*

@Entity
@Table(name = "users")
data class UserEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val userId: Long,
    val name: String
)

@Entity
@Table(name = "credentials")
data class Credential(
    @EmbeddedId
    val id: CredentialId,
    val credential: String
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