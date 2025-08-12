package mikhail.shell.video.hosting.domain

import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.geometry.Positions
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.min

fun findFileByName(directory: File, fileName: String): File? {
    return directory.listFiles { dir, name ->
        name.parseFileName() == fileName
    }?.firstOrNull()
}
fun String.parseExtension(): String {
    return substringAfterLast(".", "")
}
fun String.parseFileName(): String {
    return substringBeforeLast(".")
}



fun uploadImage(
    uploadedFile: UploadedFile,
    targetFile: String,
    width: Int = 500,
    height: Int = 280
) = uploadImage(
    uploadedFile = uploadedFile,
    targetFile = File(targetFile),
    width = width,
    height = height
)

fun uploadImage(
    uploadedFile: UploadedFile,
    targetFile: File,
    width: Int = 0,
    height: Int = 0
): Boolean {
    return try {
        uploadedFile.inputStream.use {
            val inputImage = ImageIO.read(it)
            val outputImage = inputImage.crop(width, height)
            targetFile.outputStream().use {
                ImageIO.write(outputImage, targetFile.extension, it)
            }
        }
        true
    } catch (_: Exception) {
        false
    }
}

fun BufferedImage.crop(
    width: Int = this.width,
    height: Int = this.height
): BufferedImage {
    val actualRatio = this.width.toFloat() / this.height
    val targetRatio = width.toFloat() / height
    val cropWidth: Int
    val cropHeight: Int
    if (targetRatio > actualRatio) {
        cropWidth = this.width
        cropHeight = (cropWidth / targetRatio).toInt()
    } else {
        cropHeight = this.height
        cropWidth = (cropHeight / targetRatio).toInt()
    }
    return Thumbnails.of(this)
        .sourceRegion(Positions.CENTER, cropWidth, cropHeight)
        .size(min(cropWidth, width), min(cropHeight, height))
        .outputQuality(0.8f)
        .asBufferedImage()
}

