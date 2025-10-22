package mikhail.shell.video.hosting.repository

import mikhail.shell.video.hosting.entities.PendingVideoEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PendingVideosRepository: JpaRepository<PendingVideoEntity, UUID>