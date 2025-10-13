package mikhail.shell.video.hosting.dto

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

fun CommentDto.toDomain() = Comment(
    commentId = commentId,
    videoId = videoId,
    userId = userId,
    dateTime = dateTime,
    text = text
)
fun Comment.toDto() = CommentDto(
    commentId = commentId,
    videoId = videoId,
    userId = userId,
    dateTime = dateTime,
    text = text
)

fun CommentWithUserDto.toDomain() = CommentWithUser(
    comment = comment.toDomain(),
    user = user.toDomain()
)
fun CommentWithUser.toDto() = CommentWithUserDto(
    comment = comment.toDto(),
    user = user.toDto()
)