package mikhail.shell.video.hosting.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface AuthDetailRepository: JpaRepository<AuthDetailEntity, AuthDetailEntityId> {
    fun findById_UserId(userId: Long): List<AuthDetailEntity>
    fun findByUserNameAndId_Method(userName: String, method: AuthenticationMethod): Optional<AuthDetailEntity>
    fun existsByUserNameAndId_Method(userName: String, method: AuthenticationMethod): Boolean
    fun findByUserName(userName: String): Optional<AuthDetailEntity>
}