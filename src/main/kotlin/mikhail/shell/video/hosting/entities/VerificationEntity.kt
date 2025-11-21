package mikhail.shell.video.hosting.entities

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.Instant

@Entity(name = "verification_codes")
data class VerificationEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val codeId: Long? = null,
    val userName: String,
    @Enumerated(EnumType.STRING)
    val purpose: VerificationCodePurpose,
    val issuedAt: Instant,
    val code: String
)

enum class VerificationCodePurpose {
    SIGN_UP, RESET, MFA
}