package mikhail.shell.video.hosting.elastic.repository

import mikhail.shell.video.hosting.repository.entities.VideoEntity
import mikhail.shell.video.hosting.repository.entities.VideoState
import org.springframework.data.domain.Pageable
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import org.springframework.stereotype.Repository

@Repository("videoRepository_elastic")
interface VideoSearchRepository: ElasticsearchRepository<VideoEntity, Long>