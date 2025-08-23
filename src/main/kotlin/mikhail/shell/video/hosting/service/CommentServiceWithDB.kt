package mikhail.shell.video.hosting.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import jakarta.transaction.Transactional
import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.dto.toDto
import mikhail.shell.video.hosting.repository.CommentRepository
import mikhail.shell.video.hosting.repository.CommentWithUserRepository
import mikhail.shell.video.hosting.entities.toDomain
import mikhail.shell.video.hosting.entities.toEntity
import mikhail.shell.video.hosting.repository.VideoRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class CommentServiceWithDB @Autowired constructor(
    private val commentRepository: CommentRepository,
    private val commentWithUserRepository: CommentWithUserRepository,
    private val videoRepository: VideoRepository,
    private val fcm: FirebaseMessaging,
    private val objectMapper: ObjectMapper
): CommentService {
    @Value("\${video-hosting.server.host}")
    private lateinit var HOST: String
    @Value("\${server.port}")
    private lateinit var PORT: String
    override fun post(comment: Comment) {
        if (!videoRepository.existsById(comment.videoId)) {
            throw NoSuchElementException()
        }
        val savedCommentEntity = commentRepository.save(comment.toEntity())
        sendMessage(savedCommentEntity.commentId!!, Action.ADD)
    }

    @Transactional
    override fun removeAllByUserId(userId: Long): Boolean {
        commentRepository.deleteByUserId(userId)
        return commentRepository.existsByUserId(userId)
    }

    override fun remove(userId: Long, commentId: Long) {
        val commentEntity = commentRepository
            .findById(commentId)
            .orElseThrow()
        if (userId != commentEntity.userId) {
            throw IllegalAccessException()
        }
        commentRepository.delete(commentEntity)
        sendMessage(commentId, Action.REMOVE)
    }

    override fun get(videoId: Long, before: Instant): List<CommentWithUser> {
        return commentWithUserRepository
            .findByVideoIdAndDateTimeBeforeOrderByDateTimeDesc(videoId = videoId, before = before)
            .map { it.toDomain() }
    }

    override fun edit(comment: Comment) {
        val commentEntity = commentRepository.findById(comment.commentId!!).orElseThrow()
        if (comment.userId != commentEntity.userId) {
            throw IllegalAccessException()
        }
        commentRepository.save(
            comment
                .copy(videoId = commentEntity.videoId)
                .toEntity()
        )
        sendMessage(comment.commentId, action = Action.UPDATE)
    }

    private fun sendMessage(commentId: Long, action: Action) {
        val commentWithUserEntity = commentWithUserRepository.findById(commentId).orElseThrow()
        val userId = commentWithUserEntity.userId
        val commentWithUser = commentWithUserEntity.toDomain().toDto(avatar = "https://$HOST:$PORT/api/v1/users/$userId/avatar")
        val videoId = commentWithUser.comment.videoId
        val topic = "videos.$videoId.comments"
        val actionModel = ActionModel(action, commentWithUser)
        val mappedData = mapOf("actionModel" to actionModel.toJson())
        val message = Message.builder()
            .setTopic(topic)
            .putAllData(mappedData)
            .build()
        fcm.send(message)
    }
    private fun Any.toJson(): String {
        return objectMapper.writeValueAsString(this)
    }
}