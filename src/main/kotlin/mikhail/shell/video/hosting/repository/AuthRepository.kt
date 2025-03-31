package mikhail.shell.video.hosting.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface AuthRepository: JpaRepository<Credential, CredentialId> {
    fun findById_UserId(userId: Long): List<Credential>
    fun findByUserNameAndId_Method(userName: String, method: AuthenticationMethod): Optional<Credential>
    fun existsByUserNameAndId_Method(userName: String, method: AuthenticationMethod): Boolean
}