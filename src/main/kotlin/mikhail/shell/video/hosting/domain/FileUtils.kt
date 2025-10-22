package mikhail.shell.video.hosting.domain

import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.geometry.Positions
import java.awt.image.BufferedImage
import java.io.File
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.Paths
import javax.imageio.ImageIO
import kotlin.io.path.Path
import kotlin.io.path.pathString
import kotlin.math.min

fun findFileByName(directory: Path, fileName: String): File? {
    return findFileByName(directory.pathString, fileName)
}

fun findFileByName(directory: String, fileName: String): File? {
    return findFileByName(Path(directory).toFile(), fileName)
}

fun findFileByName(directory: File, fileName: String): File? {
    return directory.listFiles { _, name -> name.parseFileName() == fileName }?.firstOrNull()
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
    width: Int = -1,
    height: Int = -1,
) = uploadImage(
    uploadedFile = uploadedFile,
    targetFile = File(targetFile),
    width = width,
    height = height
)

fun uploadImage(
    uploadedFile: File,
    targetFile: String,
    width: Int = -1,
    height: Int = -1,
) = uploadImage(
    uploadedFile = uploadedFile.inputStream(),
    targetFile = File(targetFile),
    width = width,
    height = height
)

fun uploadImage(
    uploadedFile: InputStream,
    targetFile: File,
    width: Int = -1,
    height: Int = -1
): Boolean {
    return try {
        val inputImage = ImageIO.read(uploadedFile)
        val outputImage = when {
            width == -1 && height == -1 -> inputImage
            else -> inputImage.crop(
                width = if (width > 0) width else inputImage.width,
                height = if (height > 0) height else inputImage.height
            )
        }
        targetFile.outputStream().use {
            ImageIO.write(outputImage, targetFile.extension, it)
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun uploadImage(
    uploadedFile: UploadedFile,
    targetFile: File,
    width: Int = -1,
    height: Int = -1,
) = uploadImage(
    uploadedFile = uploadedFile.bytes.inputStream(),
    targetFile = targetFile,
    width = width,
    height = height
)

fun BufferedImage.crop(
    width: Int = this.width,
    height: Int = this.height,
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

