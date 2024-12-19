package mikhail.shell.video.hosting.controllers

import mikhail.shell.video.hosting.domain.ChannelInfo
import mikhail.shell.video.hosting.dto.ExtendedChannelInfo
import mikhail.shell.video.hosting.service.ChannelService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.query.Param
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/channels")
class ChannelController @Autowired constructor(
    private val channelService: ChannelService
) {
    @GetMapping("/{channelId}")
    fun provideChannelInfo(
        @PathVariable channelId: Long
    ): ResponseEntity<ChannelInfo> {
        return ResponseEntity.status(HttpStatus.OK).body(channelService.provideChannelInfo(channelId))
    }
    @GetMapping("/{channelId}/extended")
    fun providedExtendedChannelInfo(
        @PathVariable channelId: Long,
        @Param("userId") userId: Long
    ): ResponseEntity<ExtendedChannelInfo> {
        return ResponseEntity.status(HttpStatus.OK).body(channelService.providedExtendedChannelInfo(channelId, userId))
    }
}