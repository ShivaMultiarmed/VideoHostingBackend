package mikhail.shell.video.hosting.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.firebase.messaging.FirebaseMessaging
import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.repository.CommentRepository
import mikhail.shell.video.hosting.repository.CommentWithUserRepository
import mikhail.shell.video.hosting.entities.toDomain
import mikhail.shell.video.hosting.entities.toEntity
import mikhail.shell.video.hosting.repository.VideoRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class CommentServiceWithDB @Autowired constructor(
    private val commentRepository: CommentRepository,
    private val commentWithUserRepository: CommentWithUserRepository,
    private val videoRepository: VideoRepository,
    private val fcm: FirebaseMessaging,
    private val objectMapper: ObjectMapper,
) : CommentService {
    @Value("\${video-hosting.server.host}")
    private lateinit var HOST: String

    @Value("\${server.port}")
    private lateinit var PORT: String
    override fun post(comment: Comment): Comment {
        if (!videoRepository.existsById(comment.videoId)) {
            throw NoSuchElementException()
        }
        return commentRepository.save(comment.toEntity()).toDomain()
    }

    override fun removeAllByUserId(userId: Long): Boolean {
        commentRepository.deleteByUserId(userId)
        return commentRepository.existsByUserId(userId)
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

    override fun edit(comment: Comment): Comment {
        val commentEntity = commentRepository.findById(comment.commentId!!).orElseThrow()
        if (comment.userId != commentEntity.userId) {
            throw IllegalAccessException()
        }
        return commentRepository.save(
            comment.copy(videoId = commentEntity.videoId).toEntity()
        ).toDomain()
    }

    override fun get(commentId: Long): Comment {
        return commentRepository.findById(commentId).orElseThrow().toDomain()
    }
}