package mikhail.shell.video.hosting.repository

import mikhail.shell.video.hosting.repository.entities.VideoWithChannelEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface VideoWithChannelsRepository: JpaRepository<VideoWithChannelEntity, Long>