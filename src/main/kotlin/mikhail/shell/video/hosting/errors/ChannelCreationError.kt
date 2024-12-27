package mikhail.shell.video.hosting.errors

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class ChannelCreationError: Error {
    EXISTS,
    UNEXPECTED,
    TITLE_EMPTY,
    DESCRIPTION_EMPTY
}