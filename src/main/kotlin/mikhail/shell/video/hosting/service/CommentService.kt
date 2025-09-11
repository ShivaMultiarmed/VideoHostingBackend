package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.Comment
import mikhail.shell.video.hosting.domain.CommentWithUser
import java.time.Instant

interface CommentService {
    fun get(commentId: Long): Comment
    fun post(comment: Comment): CommentWithUser
    fun get(videoId: Long, before: Instant, partSize: Int): List<CommentWithUser>
    fun remove(userId: Long, commentId: Long)
    fun removeAllByUserId(userId: Long): Boolean
    fun edit(comment: Comment): CommentWithUser
}