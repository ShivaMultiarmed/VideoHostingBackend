package mikhail.shell.video.hosting.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import jakarta.transaction.Transactional
import mikhail.shell.video.hosting.domain.*
import mikhail.shell.video.hosting.dto.toDto
import mikhail.shell.video.hosting.errors.CommentError
import mikhail.shell.video.hosting.errors.ValidationException
import mikhail.shell.video.hosting.repository.CommentRepository
import mikhail.shell.video.hosting.repository.CommentWithUserRepository
import mikhail.shell.video.hosting.repository.entities.toDomain
import mikhail.shell.video.hosting.repository.entities.toEntity
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class CommentServiceWithDB @Autowired constructor(
    private val commentRepository: CommentRepository,
    private val commentWithUserRepository: CommentWithUserRepository,
    private val fcm: FirebaseMessaging,
    private val objectMapper: ObjectMapper
): CommentService {
    @Value("\${hosting.server.host}")
    private lateinit var HOST: String
    @Value("\${server.port}")
    private lateinit var PORT: String
    override fun save(comment: Comment) {
        if (comment.text.length > ValidationRules.MAX_TEXT_LENGTH) {
            throw ValidationException(CommentError.TEXT_TOO_LARGE)
        }
        val action = if (comment.commentId != null && commentRepository.existsById(comment.commentId)) Action.UPDATE else Action.ADD
        val commentEntity = comment
            .toEntity()
            .let {
                it.copy(
                    commentId = if (it.commentId != null && !commentRepository.existsById(it.commentId!!)) null else it.commentId,
                    dateTime = if (it.commentId == null) Instant.now() else commentRepository.findById(it.commentId!!).orElseThrow().dateTime
                )
            }

        val savedCommentEntity = commentRepository.save(commentEntity)
        sendMessage(savedCommentEntity.commentId!!, action)
    }

    override fun checkExistence(commentId: Long): Boolean {
        return commentRepository.existsById(commentId)
    }

    @Transactional
    override fun removeAllByUserId(userId: Long): Boolean {
        commentRepository.deleteByUserId(userId)
        return commentRepository.existsByUserId(userId)
    }

    override fun remove(commentId: Long) {
        if (!commentRepository.existsById(commentId)) {
            throw NoSuchElementException()
        }
        sendMessage(commentId, Action.REMOVE)
        commentRepository.deleteById(commentId)
    }

    override fun checkOwner(userId: Long, commentId: Long): Boolean {
        return commentWithUserRepository.existsByUserIdAndCommentId(userId, commentId)
    }

    override fun get(videoId: Long, before: Instant): List<CommentWithUser> {
        val commentEntities = commentWithUserRepository.findByVideoIdAndDateTimeBeforeOrderByDateTimeDesc(videoId, before)
        return commentEntities.map { it.toDomain() }
    }

    private fun sendMessage(commentId: Long, action: Action = Action.ADD) {
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