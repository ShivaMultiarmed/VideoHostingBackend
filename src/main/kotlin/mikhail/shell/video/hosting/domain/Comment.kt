package mikhail.shell.video.hosting.domain

import java.time.Instant

data class Comment(
    val commentId: Long,
    val videoId: Long,
    val userId: Long,
    val dateTime: Instant,
    val text: String
)

data class CommentWithUser(
    val comment: Comment,
    val user: User
)

data class CommentCreationModel(
    val videoId: Long,
    val userId: Long,
    val text: String
)

data class CommentEditingModel(
    val commentId: Long,
    val userId: Long,
    val text: String
)