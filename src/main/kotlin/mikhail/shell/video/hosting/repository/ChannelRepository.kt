package mikhail.shell.video.hosting.repository

import mikhail.shell.video.hosting.domain.ChannelInfo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChannelRepository : JpaRepository<ChannelInfo, Long>