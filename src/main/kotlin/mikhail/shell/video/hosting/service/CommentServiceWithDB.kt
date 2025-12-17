package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.entities.CommentEntity
import mikhail.shell.video.hosting.entities.toDomain
import mikhail.shell.video.hosting.repository.CommentRepository
import mikhail.shell.video.hosting.repository.CommentWithUserRepository
import mikhail.shell.video.hosting.repository.VideoRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class CommentServiceWithDB @Autowired constructor(
    private val commentRepository: CommentRepository,
    private val commentWithUserRepository: CommentWithUserRepository,
    private val videoRepository: VideoRepository
) : CommentService {
    override fun post(comment: CommentCreationModel): CommentWithUser {
        if (!videoRepository.existsById(comment.videoId)) {
            throw NoSuchElementException()
        }
        val postedComment = commentRepository.save(
            CommentEntity(
                videoId = comment.videoId,
                userId = comment.userId,
                text = comment.text
            )
        ).toDomain()
        return commentWithUserRepository.findById(postedComment.commentId).orElseThrow().toDomain()
    }

    override fun removeAllByUserId(userId: Long): Boolean {
        commentRepository.deleteByUserId(userId)
        return !commentRepository.existsByUserId(userId)
    }

    override fun remove(userId: Long, commentId: Long) {
        val commentEntity = commentRepository.findById(commentId).orElseThrow()
        if (userId != commentEntity.userId) {
            throw IllegalAccessException()
        }
        commentRepository.delete(commentEntity)
    }

    override fun get(videoId: Long, before: Instant, partSize: Int): List<CommentWithUser> {
        return commentWithUserRepository.findByVideoIdAndDateTimeBeforeOrderByDateTimeDesc(
            videoId = videoId,
            before = before,
            pageable = PageRequest.of(0, partSize)
        ).map { it.toDomain() }
    }

    override fun edit(comment: CommentEditingModel): CommentWithUser {
        val commentEntity = commentRepository.findById(comment.commentId).orElseThrow()
        if (comment.userId != commentEntity.userId) {
            throw IllegalAccessException()
        }
        commentRepository.save(commentEntity.copy(text = comment.text))
        return commentWithUserRepository.findById(comment.commentId).orElseThrow().toDomain()
    }

    override fun get(commentId: Long): Comment {
        return commentRepository.findById(commentId).orElseThrow().toDomain()
    }
}