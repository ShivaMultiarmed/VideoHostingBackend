package mikhail.shell.video.hosting.controllers

import mikhail.shell.video.hosting.dto.CommentDto
import mikhail.shell.video.hosting.dto.toDomain
import mikhail.shell.video.hosting.errors.CompoundError
import mikhail.shell.video.hosting.errors.CreateCommentError
import mikhail.shell.video.hosting.errors.HostingDataException
import mikhail.shell.video.hosting.service.CommentService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/comments")
class CommentController @Autowired constructor(
    private val commentService: CommentService
) {
    @PostMapping("/post")
    fun create(@RequestBody commentDto: CommentDto): ResponseEntity<Unit> {
        val comment = commentDto.toDomain()
        val compoundError = CompoundError<CreateCommentError>()
        if (comment.text.isEmpty()) {
            compoundError.add(CreateCommentError.TEXT_EMPTY)
        }
        if (compoundError.isNotNull()) {
            throw HostingDataException(compoundError)
        }
        commentService.create(comment)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }
}