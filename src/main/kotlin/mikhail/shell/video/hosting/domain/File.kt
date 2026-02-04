package mikhail.shell.video.hosting.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.geometry.Positions
import org.springframework.http.MediaTypeFactory
import java.awt.image.BufferedImage
import java.io.File
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.Path
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

suspend fun uploadImage(
    uploadedFile: Path,
    targetFile: Path,
    width: Int = -1,
    height: Int = -1,
    compress: Boolean = false
): Boolean {
    return uploadedFile.toFile().inputStream().use {
        uploadImage(
            uploadedFile = it,
            targetFile = targetFile,
            width = width,
            height = height,
            compress = compress
        )
    }
}

suspend fun uploadImage(
    uploadedFile: UploadedFile,
    targetFile: Path,
    width: Int = -1,
    height: Int = -1,
    compress: Boolean = false
): Boolean {
    return uploadedFile.content.inputStream().use {
        uploadImage(
            uploadedFile = it,
            targetFile = targetFile,
            width = width,
            height = height,
            compress = compress
        )
    }
}

internal suspend fun uploadImage(
    uploadedFile: InputStream,
    targetFile: Path,
    width: Int = -1,
    height: Int = -1,
    compress: Boolean = false
): Boolean {
    return try {
        val inputImage = withContext(Dispatchers.IO) {
            uploadedFile.toImage()!!
        }
        val outputImage = when {
            width == -1 && height == -1 -> inputImage
            else -> withContext(Dispatchers.Default) {
                inputImage.crop(
                    width = if (width > 0) width else inputImage.width,
                    height = if (height > 0) height else inputImage.height
                )
            }
        }
        withContext(Dispatchers.IO) {
            outputImage.save(
                output = targetFile,
                compressionCoefficient = when (compress) {
                    true -> outputImage.compressionCoefficient
                    false -> 1f
                }
            )
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun InputStream.toImage(): BufferedImage? {
    return try {
        Thumbnails.of(this)
            .useExifOrientation(true)
            .scale(1.0)
            .asBufferedImage()
    } catch (_: Exception) {
        null
    }
}

fun BufferedImage.crop(
    width: Int = this.width,
    height: Int = this.height
): BufferedImage {
    val actualRatio = this.width.toFloat() / this.height
    val cropRatio = width.toFloat() / height
    val cropWidth: Int
    val cropHeight: Int
    if (cropRatio > actualRatio) {
        cropWidth = this.width
        cropHeight = (cropWidth / cropRatio).toInt()
    } else {
        cropHeight = this.height
        cropWidth = (cropHeight * cropRatio).toInt()
    }
    return Thumbnails.of(this)
        .sourceRegion(Positions.CENTER, cropWidth, cropHeight)
        .size(min(cropWidth, width), min(cropHeight, height))
        .asBufferedImage()
}

fun BufferedImage.save(
    output: Path,
    compressionCoefficient: Float = this.compressionCoefficient
) {
    return Thumbnails.of(this)
        .scale(1.0)
        .outputQuality(compressionCoefficient)
        .toFile(output.toFile())
}

val BufferedImage.compressionCoefficient: Float
    get() {
        return when {
            width > 1024 || height > 1024 -> 0.7f
            width > 512 || height > 512 -> 0.8f
            width > 128 || height > 128 -> 0.9f
            else -> 1f
        }
    }