package mikhail.shell.video.hosting.repository

import mikhail.shell.video.hosting.repository.entities.RecoveryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface RecoveryRepository: JpaRepository<RecoveryEntity, Long> {
    fun findByUserId(userId: Long): Optional<RecoveryEntity>
}