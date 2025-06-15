package mikhail.shell.video.hosting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import mikhail.shell.video.hosting.domain.Comment
import mikhail.shell.video.hosting.domain.CommentWithUser
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CommentDto(
    val commentId: Long? = null,
    val videoId: Long,
    val userId: Long,
    val dateTime: Instant? = null,
    val text: String
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CommentWithUserDto(
    val comment: CommentDto,
    val user: UserDto
)

fun CommentDto.toDomain() = Comment(commentId, videoId, userId, dateTime, text)
fun Comment.toDto() = CommentDto(commentId, videoId, userId, dateTime, text)

fun CommentWithUserDto.toDomain() = CommentWithUser(comment.toDomain(), user.toDomain())
fun CommentWithUser.toDto(avatar: String? = null) = CommentWithUserDto(comment.toDto(), user.toDto(avatar = avatar))