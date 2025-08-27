package mikhail.shell.video.hosting.controllers

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import mikhail.shell.video.hosting.domain.Comment
import mikhail.shell.video.hosting.domain.ValidationRules
import mikhail.shell.video.hosting.dto.CommentWithUserDto
import mikhail.shell.video.hosting.dto.toDto
import mikhail.shell.video.hosting.service.CommentService
import mikhail.shell.video.hosting.service.VideoService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
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
        @Validated @ModelAttribute request: CommentCreationRequest,
        @AuthenticationPrincipal userId: Long,
    ) {
        commentService.post(
            Comment(
                videoId = request.videoId,
                userId = userId,
                text = request.text,
                dateTime = Instant.now()
            )
        )
    }
    @PatchMapping
    fun edit(
        @Validated @ModelAttribute request: CommentEditingRequest,
        @AuthenticationPrincipal userId: Long
    ) {
        commentService.edit(
            Comment(
                commentId = request.commentId,
                userId = userId,
                text = request.text,
                videoId = request.videoId
            )
        )
    }
    @GetMapping("/videos/{videoId}")
    fun get(
        request: HttpServletRequest,
        @PathVariable videoId: Long,
        @RequestParam before: Instant
    ): List<CommentWithUserDto> {
        return commentService
            .get(videoId = videoId, before = before)
            .map { it.toDto(avatar = "$BASE_URL/users/${it.user.userId}/avatar") }
    }
    @DeleteMapping("/{commentId}")
    fun remove(
        @RequestParam @Positive commentId: Long,
        @AuthenticationPrincipal userId: Long
    ) {
        commentService.remove(userId,commentId)
    }
}

data class CommentCreationRequest(
    @field:Positive
    val videoId: Long,
    @field:Positive
    val userId: Long,
    @field:NotBlank @Max(ValidationRules.MAX_TEXT_LENGTH.toLong())
    val text: String
)

data class CommentEditingRequest(
    @field:Positive
    val commentId: Long,
    @field:Positive
    val videoId: Long,
    @field:NotBlank @Max(ValidationRules.MAX_TEXT_LENGTH.toLong())
    val text: String
)