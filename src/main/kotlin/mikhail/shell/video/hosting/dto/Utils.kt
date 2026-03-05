package mikhail.shell.video.hosting.dto

import mikhail.shell.video.hosting.domain.UploadedFile
import org.springframework.web.multipart.MultipartFile

inline fun String.camelToSnakeCase(): String {
    return replace(Regex("[A-Z]")) {
        "_" + it.value.lowercase()
    }
}

fun MultipartFile.toUploadedFile() = UploadedFile(
    name = originalFilename ?: "unnamed",
    mimeType = contentType ?: "application/octet-stream",
    content = inputStream.use { it.readBytes() }
)