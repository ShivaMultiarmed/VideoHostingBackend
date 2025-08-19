package mikhail.shell.video.hosting.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PasswordRepository: JpaRepository<PasswordEntity, Long>