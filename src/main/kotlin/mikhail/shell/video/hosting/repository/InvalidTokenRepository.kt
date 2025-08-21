package mikhail.shell.video.hosting.repository

import mikhail.shell.video.hosting.entities.InvalidTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface InvalidTokenRepository: JpaRepository<InvalidTokenEntity, String>