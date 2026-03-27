package mikhail.shell.video.hosting.domain

import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.geometry.Positions
import java.awt.image.BufferedImage
import java.io.File
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.outputStream
import kotlin.math.min

data class UploadedFile(
    val name: String,
    val mimeType: String,
    val content: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (javaClass != other?.javaClass) return false
        if (this === other) return true

        other as UploadedFile

        return name == other.name
                && mimeType == other.mimeType
                && content.contentEquals(other.content)
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + content.contentHashCode()
        return result
    }
}

fun findFileByName(directory: Path, fileName: String): File? {
    return findFileByName(directory.toString(), fileName)
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

fun InputStream.uploadFile(targetFile: Path): Boolean {
    return try {
        targetFile.outputStream().use(::copyTo)
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun BufferedImage.uploadImage(
    targetFile: Path,
    targetWidth: Int = width,
    targetHeight: Int = height,
    compress: Boolean = false
): Boolean {
    return try {
        require(targetWidth > 0 && targetHeight > 0)
        Thumbnails.of(this)
            .let {
                when {
                    targetWidth == width && targetHeight == height -> it
                    else -> {
                        val (croppingWidth, croppingHeight) = evaluateCropDimensions(
                            targetWidth,
                            targetHeight
                        )
                        it.crop(
                            targetWidth = targetWidth,
                            targetHeight = targetHeight,
                            cropWidth = croppingWidth,
                            cropHeight = croppingHeight
                        )
                    }
                }
            }
            .save(
                output = targetFile,
                compressionCoefficient = when (compress) {
                    true -> evaluateCompressionCoefficient(targetWidth, targetHeight)
                    false -> 1f
                }
            )
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun InputStream.toImage(): BufferedImage? {
    return try {
        use { inputStream ->
            Thumbnails.of(inputStream)
                .useExifOrientation(true)
                .scale(1.0)
                .asBufferedImage()
        }
    } catch (_: Exception) {
        null
    }
}

private fun Thumbnails.Builder<BufferedImage>.crop(
    targetWidth: Int,
    targetHeight: Int,
    cropWidth: Int,
    cropHeight: Int
): Thumbnails.Builder<BufferedImage> {
    return sourceRegion(Positions.CENTER, cropWidth, cropHeight)
        .size(min(targetWidth, cropWidth), min(targetHeight, cropHeight))
}

private fun Thumbnails.Builder<BufferedImage>.save(
    output: Path,
    compressionCoefficient: Float
) {
    return outputQuality(compressionCoefficient)
        .toFile(output.toFile())
}

private fun BufferedImage.evaluateCropDimensions(
    targetWidth: Int,
    targetHeight: Int
): Pair<Int, Int> {
    val actualRatio = width.toFloat() / height
    val cropRatio = targetWidth.toFloat() / targetHeight
    val cropWidth: Int
    val cropHeight: Int
    if (cropRatio > actualRatio) {
        cropWidth = width
        cropHeight = (cropWidth / cropRatio).toInt()
    } else {
        cropHeight = height
        cropWidth = (cropHeight * cropRatio).toInt()
    }
    return cropWidth to cropHeight
}

private fun evaluateCompressionCoefficient(width: Int, height: Int): Float {
    return when {
        width > 1024 || height > 1024 -> 0.7f
        width > 512 || height > 512 -> 0.8f
        width > 128 || height > 128 -> 0.9f
        else -> 1f
    }
}