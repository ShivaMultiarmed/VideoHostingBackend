package mikhail.shell.video.hosting.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import mikhail.shell.video.hosting.domain.Comment
import mikhail.shell.video.hosting.repository.CommentRepository
import mikhail.shell.video.hosting.repository.CommentWithUserRepository
import mikhail.shell.video.hosting.repository.entities.toDomain
import mikhail.shell.video.hosting.repository.entities.toEntity
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CommentServiceWithDB @Autowired constructor(
    private val commentRepository: CommentRepository,
    private val commentWithUserRepository: CommentWithUserRepository,
    private val fcm: FirebaseMessaging,
    private val objectMapper: ObjectMapper
): CommentService {
    override fun create(comment: Comment) {
        val commentEntity = comment.toEntity()
        val createdCommentEntity = commentRepository.save(commentEntity)
        sendMessage(createdCommentEntity.commentId!!)
    }
    private fun sendMessage(commentId: Long) {
        val commentWithUserEntity = commentWithUserRepository.findById(commentId).orElseThrow()
        val commentWithUser = commentWithUserEntity.toDomain()
        val topic = resolveTemplate(COMMENT_TOPIC_TEMPLATE, mapOf("video_id" to commentWithUser.comment.videoId))
        val mappedComment = commentWithUser.toNestedStringMap()
        val message = Message.builder()
            .setTopic(topic)
            .putAllData(mappedComment)
            .build()
        fcm.send(message)
    }
    fun Any.toNestedStringMap(): Map<String, String> {
        val rawMap: Map<String, Any?> = objectMapper.convertValue(
            this,
            object : TypeReference<Map<String, Any?>>() {}
        )
        return convertMapValuesToString(rawMap)
    }

    private fun convertMapValuesToString(map: Map<String, Any?>): Map<String, String> {
        return map.mapValues { (_, value) ->
            when (value) {
                null -> "null"
                is Map<*, *> -> convertMapValuesToString(value as Map<String, Any?>).toString()
                is Collection<*> -> value.joinToString(",")
                else -> value.toString()
            }
        }
    }
    private fun resolveTemplate(template: String, map: Map<String, Any>): String {
        var result = template
        for ((key, value) in map.entries) {
            result = result.replace("{$key}", value.toString())
        }
        return result
    }
    companion object {
        private const val COMMENT_TOPIC_TEMPLATE = "videos.{video_id}.comments"
    }
}