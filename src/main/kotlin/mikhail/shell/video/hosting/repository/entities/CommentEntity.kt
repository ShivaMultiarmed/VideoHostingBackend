package mikhail.shell.video.hosting.repository.entities

import jakarta.persistence.*
import mikhail.shell.video.hosting.domain.Comment
import mikhail.shell.video.hosting.domain.CommentWithUser
import mikhail.shell.video.hosting.repository.UserEntity
import mikhail.shell.video.hosting.repository.toDomain
import mikhail.shell.video.hosting.repository.toEntity
import java.time.Instant

@Entity
@Table(name = "comments")
data class CommentEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val commentId: Long? = null,
    val videoId: Long,
    @Column(name = "user_id")
    val userId: Long,
    val dateTime: Instant? = null,
    val text: String
)

@Entity
@Table(name = "comments")
data class CommentWithUserEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val commentId: Long? = null,
    val videoId: Long,
    @Column(name = "user_id")
    val userId: Long,
    val dateTime: Instant? = null,
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
