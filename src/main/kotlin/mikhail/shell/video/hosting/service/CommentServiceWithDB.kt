package mikhail.shell.video.hosting.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import mikhail.shell.video.hosting.domain.Action
import mikhail.shell.video.hosting.domain.ActionModel
import mikhail.shell.video.hosting.domain.Comment
import mikhail.shell.video.hosting.domain.CommentWithUser
import mikhail.shell.video.hosting.errors.CommentError
import mikhail.shell.video.hosting.errors.HostingDataException
import mikhail.shell.video.hosting.repository.CommentRepository
import mikhail.shell.video.hosting.repository.CommentWithUserRepository
import mikhail.shell.video.hosting.repository.entities.toDomain
import mikhail.shell.video.hosting.repository.entities.toEntity
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class CommentServiceWithDB @Autowired constructor(
    private val commentRepository: CommentRepository,
    private val commentWithUserRepository: CommentWithUserRepository,
    private val fcm: FirebaseMessaging,
    private val objectMapper: ObjectMapper
): CommentService {
    override fun save(comment: Comment) {
        if (comment.text.length > 200) {
            throw HostingDataException(CommentError.TEXT_TOO_LARGE)
        }
        val commentEntity = comment.toEntity()
        val exists = comment.commentId?.let { commentRepository.existsById(it) }?: false
        val action = if (!exists) Action.ADD else Action.UPDATE
        val createdCommentEntity = commentRepository.save(commentEntity)
        sendMessage(createdCommentEntity.commentId!!, action)
    }

    override fun remove(videoId: Long) {
        if (!commentRepository.existsById(videoId)) {
            throw NoSuchElementException()
        }
        sendMessage(videoId, Action.REMOVE)
        commentRepository.deleteById(videoId)
    }

    override fun get(videoId: Long, before: Instant): List<CommentWithUser> {
        val commentEntities = commentWithUserRepository.findByVideoIdAndDateTimeBeforeOrderByDateTimeDesc(videoId, before)
        return commentEntities.map { it.toDomain() }
    }

    private fun sendMessage(commentId: Long, action: Action = Action.ADD) {
        val commentWithUserEntity = commentWithUserRepository.findById(commentId).orElseThrow()
        val commentWithUser = commentWithUserEntity.toDomain()
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