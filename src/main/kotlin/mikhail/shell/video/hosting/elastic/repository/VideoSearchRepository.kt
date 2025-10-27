package mikhail.shell.video.hosting.elastic.repository

import mikhail.shell.video.hosting.elastic.documents.VideoDocument
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import org.springframework.stereotype.Repository

@Repository
interface VideoSearchRepository: ElasticsearchRepository<VideoDocument, Long>