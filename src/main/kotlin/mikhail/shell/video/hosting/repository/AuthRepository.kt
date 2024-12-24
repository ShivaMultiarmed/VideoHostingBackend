package mikhail.shell.video.hosting.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface AuthRepository: JpaRepository<Credential, CredentialId> {
    fun findByUserNameAndId_Method(userName: String, method: AuthenticationMethod): Optional<Credential>
}