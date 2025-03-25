package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.Comment

interface CommentService {
    fun create(comment: Comment)
}