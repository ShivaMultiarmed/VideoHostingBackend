package mikhail.shell.video.hosting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import mikhail.shell.video.hosting.domain.Comment
import mikhail.shell.video.hosting.domain.CommentWithUser
import java.time.Instant

data class CommentDto(
    val commentId: Long,
    val videoId: Long,
    val userId: Long,
    val dateTime: Instant,
    val text: String
)

data class CommentWithUserDto(
    val comment: CommentDto,
    val user: UserDto
)

fun CommentDto.toDomain() = Comment(commentId, videoId, userId, dateTime, text)
fun Comment.toDto() = CommentDto(commentId, videoId, userId, dateTime, text)

fun CommentWithUserDto.toDomain() = CommentWithUser(comment.toDomain(), user.toDomain())
fun CommentWithUser.toDto() = CommentWithUserDto(comment.toDto(), user.toDto())