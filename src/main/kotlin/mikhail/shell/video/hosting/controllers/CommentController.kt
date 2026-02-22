package mikhail.shell.video.hosting.controllers

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import mikhail.shell.video.hosting.domain.CommentCreationModel
import mikhail.shell.video.hosting.domain.CommentEditingModel
import mikhail.shell.video.hosting.domain.LongId
import mikhail.shell.video.hosting.domain.ValidationRules.MAX_TEXT_LENGTH
import mikhail.shell.video.hosting.dto.CommentWithUserDto
import mikhail.shell.video.hosting.dto.toDto
import mikhail.shell.video.hosting.service.CommentService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/v2/comments")
class CommentController @Autowired constructor(
    private val commentService: CommentService
) {
    @Value("\${video-hosting.server.base-url}")
    private lateinit var BASE_URL: String

    @PostMapping
    fun post(
        @Valid @RequestBody comment: CommentCreationRequest,
        @AuthenticationPrincipal userId: Long,
    ): CommentWithUserDto {
        return commentService.post(
            CommentCreationModel(
                videoId = comment.videoId,
                userId = userId,
                text = comment.text
            )
        ).toDto()
    }

    @PutMapping
    fun edit(
        @Valid @RequestBody comment: CommentEditingRequest,
        @AuthenticationPrincipal userId: Long,
    ): CommentWithUserDto {
        return commentService.edit(
            comment = CommentEditingModel(
                commentId = comment.commentId,
                userId = userId,
                text = comment.text
            )
        ).toDto()
    }

    @GetMapping("/videos/{video_id}")
    fun get(
        @PathVariable("video_id") @LongId videoId: Long,
        @RequestParam("before") before: Instant?,
        @RequestParam("part_size") partSize: Int = 10
    ): List<CommentWithUserDto> {
        return commentService.get(
            videoId = videoId,
            before = before,
            partSize = partSize
        ).map {
            it.toDto()
        }
    }

    @DeleteMapping("/{video_id}")
    fun remove(
        @PathVariable("video_id") @Positive commentId: Long,
        @AuthenticationPrincipal userId: Long,
    ) {
        commentService.remove(userId, commentId)
    }
}

data class CommentCreationRequest(
    @field:LongId
    val videoId: Long,
    @field:NotBlank @Max(MAX_TEXT_LENGTH.toLong())
    val text: String,
)

data class CommentEditingRequest(
    @field:LongId
    val commentId: Long,
    @field:NotBlank @Max(MAX_TEXT_LENGTH.toLong())
    val text: String,
)