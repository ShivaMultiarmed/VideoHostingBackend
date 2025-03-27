package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.Comment
import mikhail.shell.video.hosting.domain.CommentWithUser
import java.time.Instant

interface CommentService {
    fun save(comment: Comment)
    fun get(videoId: Long, before: Instant): List<CommentWithUser>
    fun remove(videoId: Long)
}