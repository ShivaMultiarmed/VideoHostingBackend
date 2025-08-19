package mikhail.shell.video.hosting.repository

import mikhail.shell.video.hosting.repository.entities.VerificationCodePurpose
import mikhail.shell.video.hosting.repository.entities.VerificationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface VerificationRepository: JpaRepository<VerificationEntity, Long> {
    fun findByUserNameAndPurpose(userName: String, purpose: VerificationCodePurpose): Optional<VerificationEntity>
}