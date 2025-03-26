package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.Comment
import mikhail.shell.video.hosting.domain.CommentWithUser
import java.time.Instant
import java.time.LocalDateTime

interface CommentService {
    fun create(comment: Comment)
    fun get(videoId: Long, before: Instant): List<CommentWithUser>
}