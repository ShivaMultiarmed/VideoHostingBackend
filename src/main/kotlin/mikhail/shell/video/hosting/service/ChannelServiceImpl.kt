package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.ChannelInfo
import mikhail.shell.video.hosting.repository.models.SubscriberId
import mikhail.shell.video.hosting.dto.ChannelDto
import mikhail.shell.video.hosting.repository.ChannelRepository
import mikhail.shell.video.hosting.repository.SubscriberRepository
import mikhail.shell.video.hosting.repository.models.Subscriber
import mikhail.shell.video.hosting.repository.models.toDomain
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ChannelServiceImpl @Autowired constructor(
    private val channelRepository: ChannelRepository,
    private val subscriberRepository: SubscriberRepository
) : ChannelService {

    override fun provideChannelInfo(
        channelId: Long
    ): ChannelInfo {
        return channelRepository.findById(channelId).orElseThrow().toDomain()
    }

    override fun checkIfSubscribed(channelId: Long, userId: Long): Boolean {
        val id = SubscriberId(channelId, userId)
        return subscriberRepository.existsById(id)
    }
}