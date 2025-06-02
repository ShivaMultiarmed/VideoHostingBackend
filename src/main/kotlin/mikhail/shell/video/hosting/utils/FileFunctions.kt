package mikhail.shell.video.hosting.utils

import org.springframework.http.MediaType
import org.springframework.http.MediaTypeFactory

fun getMimeType(extension: String): String {
    return MediaTypeFactory
        .getMediaType(extension)
        .orElse(MediaType("application", "octet-stream"))
        .toString()
}