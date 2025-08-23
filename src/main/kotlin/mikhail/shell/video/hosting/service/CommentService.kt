package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.Comment
import mikhail.shell.video.hosting.domain.CommentWithUser
import java.time.Instant

interface CommentService {
    fun post(comment: Comment)
    fun get(videoId: Long, before: Instant): List<CommentWithUser>
    fun remove(userId: Long, commentId: Long)
    fun removeAllByUserId(userId: Long): Boolean
    fun edit(comment: Comment)
}