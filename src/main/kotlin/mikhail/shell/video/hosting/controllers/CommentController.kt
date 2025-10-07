package mikhail.shell.video.hosting.controllers

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import mikhail.shell.video.hosting.domain.Comment
import mikhail.shell.video.hosting.domain.LongId
import mikhail.shell.video.hosting.domain.ValidationRules.MAX_TEXT_LENGTH
import mikhail.shell.video.hosting.dto.CommentDto
import mikhail.shell.video.hosting.dto.CommentWithUserDto
import mikhail.shell.video.hosting.dto.toDto
import mikhail.shell.video.hosting.service.CommentService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/v2/comments")
class CommentController @Autowired constructor(
    private val commentService: CommentService,
) {
    @Value("\${video-hosting.server.base-url}")
    private lateinit var BASE_URL: String

    @PostMapping
    fun post(
        @Valid @RequestBody request: CommentCreationRequest,
        @AuthenticationPrincipal userId: Long,
    ): CommentWithUserDto {
        return commentService.post(
            Comment(
                videoId = request.videoId,
                userId = userId,
                text = request.text,
                dateTime = Instant.now()
            )
        ).toDto()
    }

    @PatchMapping
    fun edit(
        @Valid @RequestBody request: CommentEditingRequest,
        @AuthenticationPrincipal userId: Long,
    ): CommentWithUserDto {
        return commentService.edit(
            Comment(
                commentId = request.commentId,
                userId = userId,
                text = request.text,
                videoId = commentService.get(request.commentId).videoId
            )
        ).toDto()
    }

    @GetMapping("/videos/{video_id}")
    fun get(
        request: HttpServletRequest,
        @PathVariable("video_id") @LongId videoId: Long,
        @RequestParam("before") before: Instant,
        @RequestParam("part_size") partSize: Int,
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