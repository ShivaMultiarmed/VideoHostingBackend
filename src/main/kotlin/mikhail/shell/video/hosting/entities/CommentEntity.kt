package mikhail.shell.video.hosting.entities

import jakarta.persistence.*
import kotlinx.datetime.Clock
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
    val dateTime: Instant = Instant.now(),
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
    val dateTime: Instant = Instant.now(),
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

fun Comment.toEntity() = CommentEntity(
    commentId = commentId,
    videoId = videoId,
    userId = userId,
    dateTime = dateTime,
    text = text
)
fun CommentEntity.toDomain() = Comment(
    commentId = commentId!!,
    videoId = videoId,
    userId = userId,
    dateTime = dateTime,
    text = text
)

fun CommentWithUser.toEntity() = CommentWithUserEntity(
    commentId = comment.commentId,
    videoId = comment.videoId,
    userId = comment.userId,
    dateTime = comment.dateTime,
    text = comment.text,
    user = user.toEntity()
)
fun CommentWithUserEntity.toDomain(): CommentWithUser {
    return CommentWithUser(
        comment = Comment(
            commentId = commentId!!,
            videoId = videoId,
            userId = userId,
            dateTime = dateTime,
            text = text
        ),
        user = user.toDomain()
    )
}
