package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.ChannelInfo
import mikhail.shell.video.hosting.domain.SubscriberId
import mikhail.shell.video.hosting.dto.ExtendedChannelInfo
import mikhail.shell.video.hosting.repository.ChannelRepository
import mikhail.shell.video.hosting.repository.SubscriberRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.query.Param
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@Service
class ChannelServiceImpl @Autowired constructor(
    private val channelRepository: ChannelRepository,
    private val subscriberRepository: SubscriberRepository
) : ChannelService {

    override fun provideChannelInfo(
        channelId: Long
    ): ChannelInfo {
        return channelRepository.findById(channelId).orElseThrow()
    }

    override fun providedExtendedChannelInfo(
        channelId: Long,
         userId: Long
    ): ExtendedChannelInfo {
        val info = provideChannelInfo(channelId)
        val subscription = subscriberRepository.existsById(SubscriberId(channelId, userId))
        return ExtendedChannelInfo(
            info = info,
            subscription = subscription
        )
    }
}