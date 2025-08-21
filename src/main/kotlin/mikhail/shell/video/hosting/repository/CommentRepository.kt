package mikhail.shell.video.hosting.repository

import mikhail.shell.video.hosting.entities.CommentEntity
import mikhail.shell.video.hosting.entities.CommentWithUserEntity
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface CommentRepository: JpaRepository<CommentEntity, Long> {
    fun deleteByUserId(userId: Long)
    fun existsByUserId(userId: Long): Boolean
}

@Repository
interface CommentWithUserRepository: JpaRepository<CommentWithUserEntity, Long> {
    fun findByVideoIdAndDateTimeBeforeOrderByDateTimeDesc(
        videoId: Long,
        before: Instant,
        pageable: Pageable = PageRequest.of(0, 10)
    ): List<CommentWithUserEntity>
    fun existsByUserIdAndCommentId(userId: Long, commentId: Long): Boolean
}
