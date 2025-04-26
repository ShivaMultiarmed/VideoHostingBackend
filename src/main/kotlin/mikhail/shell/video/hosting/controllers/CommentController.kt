package mikhail.shell.video.hosting.controllers

import mikhail.shell.video.hosting.dto.CommentDto
import mikhail.shell.video.hosting.dto.CommentWithUserDto
import mikhail.shell.video.hosting.dto.toDomain
import mikhail.shell.video.hosting.dto.toDto
import mikhail.shell.video.hosting.errors.CompoundError
import mikhail.shell.video.hosting.errors.CommentError
import mikhail.shell.video.hosting.errors.HostingDataException
import mikhail.shell.video.hosting.service.CommentService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/v1/comments")
class CommentController @Autowired constructor(
    private val commentService: CommentService
) {
    @PostMapping("/save")
    fun create(@RequestBody commentDto: CommentDto): ResponseEntity<Unit> {
        val comment = commentDto.toDomain()
        val compoundError = CompoundError<CommentError>()
        if (comment.text.isEmpty()) {
            compoundError.add(CommentError.TEXT_EMPTY)
        }
        if (compoundError.isNotNull()) {
            throw HostingDataException(compoundError)
        }
        commentService.save(comment)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }
    @GetMapping("/videos/{videoId}")
    fun get(
        @PathVariable videoId: Long,
        @RequestParam before: Instant
    ): ResponseEntity<List<CommentWithUserDto>> {
        val comments = commentService.get(videoId, before)
        val commentDtos = comments.map { it.toDto() }
        return ResponseEntity.status(HttpStatus.OK).body(commentDtos)
    }
    @DeleteMapping("/remove")
    fun remove(@RequestParam commentId: Long): ResponseEntity<Unit> {
        val userId = SecurityContextHolder.getContext().authentication.principal as Long?
            ?: return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        if (!commentService.checkOwner(userId, commentId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        commentService.remove(commentId)
        return ResponseEntity.status(HttpStatus.OK).build()
    }
}