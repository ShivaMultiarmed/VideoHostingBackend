package mikhail.shell.video.hosting.dto


data class VideoDetailsDto(
    val video: VideoWithUserDto,
    val channel: ChannelWithUserDto,
)
