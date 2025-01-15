package mikhail.shell.video.hosting.elastic.repository

import mikhail.shell.video.hosting.repository.models.VideoEntity
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import org.springframework.stereotype.Repository

@Repository("videoRepository_elastic")
interface VideoSearchRepository: ElasticsearchRepository<VideoEntity, Long> {
    fun findByTitle(title: String): List<VideoEntity>
}