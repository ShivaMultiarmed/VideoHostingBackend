package mikhail.shell.video.hosting.repository.entities

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import mikhail.shell.video.hosting.domain.Comment
import mikhail.shell.video.hosting.domain.CommentWithUser
import mikhail.shell.video.hosting.repository.UserEntity
import mikhail.shell.video.hosting.repository.toDomain
import mikhail.shell.video.hosting.repository.toEntity
import java.time.LocalDateTime

@Entity
@Table(name = "comments")
data class CommentEntity(
    val commentId: Long? = null,
    val videoId: Long,
    val userId: Long,
    val dateTime: LocalDateTime? = null,
    val text: String
)

@Entity
@Table(name = "comments")
data class CommentWithUserEntity(
    val commentId: Long? = null,
    val videoId: Long,
    val userId: Long,
    val dateTime: LocalDateTime? = null,
    val text: String,
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
        name = "user_id",
        referencedColumnName = "user_id",
        insertable = false,
        updatable = false
    )
    val user: UserEntity
)

fun Comment.toEntity() = CommentEntity(commentId, videoId, userId, dateTime, text)
fun CommentEntity.toDomain() = Comment(commentId, videoId, userId, dateTime, text)

fun CommentWithUser.toEntity() = CommentWithUserEntity(comment.commentId, comment.videoId, comment.userId, comment.dateTime, comment.text, user.toEntity())
fun CommentWithUserEntity.toDomain(): CommentWithUser {
    val comment = Comment(commentId, videoId, userId, dateTime, text)
    val user = user.toDomain()
    return CommentWithUser(comment, user)
}
