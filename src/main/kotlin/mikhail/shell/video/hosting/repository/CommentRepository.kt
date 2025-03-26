package mikhail.shell.video.hosting.repository

import mikhail.shell.video.hosting.repository.entities.CommentEntity
import mikhail.shell.video.hosting.repository.entities.CommentWithUserEntity
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDateTime

@Repository
interface CommentRepository: JpaRepository<CommentEntity, Long>

@Repository
interface CommentWithUserRepository: JpaRepository<CommentWithUserEntity, Long> {
    fun findByVideoIdAndDateTimeBefore(
        videoId: Long,
        before: Instant,
        pageable: Pageable = PageRequest.of(0, 10)
    ): List<CommentWithUserEntity>
}
