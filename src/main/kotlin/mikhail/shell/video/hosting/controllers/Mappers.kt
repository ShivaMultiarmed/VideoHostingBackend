package mikhail.shell.video.hosting.controllers

import mikhail.shell.video.hosting.domain.UploadedFile
import org.springframework.web.multipart.MultipartFile
import javax.activation.MimetypesFileTypeMap

fun MultipartFile.toUploadedFile() = UploadedFile(
    name = originalFilename?: "unnamed",
    mimeType = contentType?: "application/octet-stream",
    inputStream = inputStream
)