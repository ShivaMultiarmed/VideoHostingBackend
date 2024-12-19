package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.domain.ChannelInfo
import mikhail.shell.video.hosting.dto.ExtendedChannelInfo
import org.springframework.data.repository.query.Param
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

interface ChannelService {
    fun provideChannelInfo(
        channelId: Long
    ): ChannelInfo

    fun providedExtendedChannelInfo(
       channelId: Long,
       userId: Long
    ): ExtendedChannelInfo
}