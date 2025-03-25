package mikhail.shell.video.hosting.dto

import mikhail.shell.video.hosting.domain.Comment
import java.time.LocalDateTime

data class CommentDto(
    val commentId: Long? = null,
    val videoId: Long,
    val userId: Long,
    val dateTime: LocalDateTime? = null,
    val text: String
)

data class CommentWithUserDto(
    val comment: CommentDto,
    val user: UserDto
)

fun CommentDto.toDomain() = Comment(commentId, videoId, userId, dateTime, text)
fun Comment.toDto() = CommentDto(commentId, videoId, userId, dateTime, text)