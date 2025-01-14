package mikhail.shell.video.hosting.repository

import mikhail.shell.video.hosting.repository.models.VideoWithChannelEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface VideoWithChannelsRepository: JpaRepository<VideoWithChannelEntity, Long> {
    @Query(
        nativeQuery = true,
        value = """
            SELECT v.* FROM `videos` AS v 
            INNER JOIN `channels` AS ch 
            ON ch.channel_id = v.channel_id 
            WHERE MATCH (v.title) AGAINST (:query IN NATURAL LANGUAGE MODE)
            """
    )
    fun findByQuery(@Param("query") query: String): List<VideoWithChannelEntity>
}