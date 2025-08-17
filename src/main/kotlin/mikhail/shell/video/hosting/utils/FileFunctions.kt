package mikhail.shell.video.hosting.utils

import org.springframework.http.MediaTypeFactory

fun getMimeType(fileName: String): String {
    return MediaTypeFactory
        .getMediaType(fileName)
        .orElseThrow { IllegalArgumentException() }
        .toString()
}