package mikhail.shell.video.hosting.entities

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.Instant

@Entity(name = "verification_codes")
data class VerificationEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val username: String, // either email or tel
    val purpose: VerificationCodePurpose,
    val issuedAt: Instant,
    val code: String
)

enum class VerificationCodePurpose {
    SIGNUP, RESET, MFA
}