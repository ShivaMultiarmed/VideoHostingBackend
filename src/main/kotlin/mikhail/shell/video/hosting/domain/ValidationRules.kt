package mikhail.shell.video.hosting.domain

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import jakarta.validation.constraints.*
import mikhail.shell.video.hosting.domain.ValidationRules.FILE_NAME_REGEX
import mikhail.shell.video.hosting.domain.ValidationRules.MAX_DESCRIPTION_LENGTH
import mikhail.shell.video.hosting.domain.ValidationRules.MAX_IMAGE_SIZE
import mikhail.shell.video.hosting.domain.ValidationRules.MAX_NAME_LENGTH
import mikhail.shell.video.hosting.domain.ValidationRules.MAX_TITLE_LENGTH
import mikhail.shell.video.hosting.domain.ValidationRules.MAX_USERNAME_LENGTH
import mikhail.shell.video.hosting.domain.ValidationRules.PASSWORD_REGEX
import mikhail.shell.video.hosting.errors.FileError
import org.apache.commons.imaging.Imaging
import org.springframework.http.MediaType
import org.springframework.http.MediaTypeFactory
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import ws.schild.jave.MultimediaObject
import java.io.File
import java.nio.file.Files
import kotlin.reflect.KClass

object ValidationRules {
    const val FILE_NAME_REGEX = "^.{1,100}\\..{1,10}$"
    const val CODE_LENGTH = 4
    const val MAX_TITLE_LENGTH = 100
    const val MAX_NAME_LENGTH = 50
    const val MAX_TEXT_LENGTH = 255
    const val MAX_USERNAME_LENGTH = 50
    const val MAX_DESCRIPTION_LENGTH = 255
    const val MAX_IMAGE_SIZE = 10 * 1024 * 1024L
    const val MAX_VIDEO_SIZE = 1024 * 1024 * 1024
    const val MIN_PASSWORD_LENGTH = 8
    const val MAX_PASSWORD_LENGTH = 20
    const val PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[^a-zA-Z0-9])\\S{$MIN_PASSWORD_LENGTH,$MAX_PASSWORD_LENGTH}$"
    const val EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$"
    const val TEL_REGEX = "^\\d{8,15}\$"
}

@Target(
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.ANNOTATION_CLASS
)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [MaxFileSizeValidator::class])
annotation class MaxFileSize(
    val max: Long,
    val message: String = "LARGE",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class MaxFileSizeValidator : ConstraintValidator<MaxFileSize, MultipartFile?> {
    private var max: Long = 0
    override fun initialize(constraintAnnotation: MaxFileSize) {
        max = constraintAnnotation.max
    }

    override fun isValid(p0: MultipartFile?, p1: ConstraintValidatorContext): Boolean {
        return p0 == null || p0.size <= max
    }
}

@Target(
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.ANNOTATION_CLASS
)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [NotEmptyFileValidator::class])
annotation class NotEmptyFile(
    val message: String = "EMPTY",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class NotEmptyFileValidator : ConstraintValidator<NotEmptyFile, MultipartFile?> {
    override fun isValid(p0: MultipartFile?, p1: ConstraintValidatorContext): Boolean {
        return p0 == null || !p0.isEmpty
    }
}

@Target(
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.ANNOTATION_CLASS
)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [FileTypeValidator::class])
annotation class FileType(
    val mime: String,
    val message: String = "NOT_SUPPORTED",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class FileTypeValidator : ConstraintValidator<FileType, MultipartFile?> {
    private var mime: String = ""

    override fun initialize(constraintAnnotation: FileType) {
        mime = constraintAnnotation.mime
    }

    override fun isValid(p0: MultipartFile?, p1: ConstraintValidatorContext?): Boolean {
        return p0 == null || !p0.isEmpty && p0.contentType?.startsWith(mime) ?: false
    }
}

@Target(
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.ANNOTATION_CLASS
)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [FileNameValidator::class])
annotation class FileName(
    val message: String = "NOT_VALID",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class FileNameValidator : ConstraintValidator<FileName, MultipartFile?> {
    override fun isValid(p0: MultipartFile?, p1: ConstraintValidatorContext?): Boolean {
        return p0 == null || p0.originalFilename?.matches(FILE_NAME_REGEX.toRegex()) ?: false
    }
}

@FileName
@NotEmptyFile
@MaxFileSize(max = MAX_IMAGE_SIZE)
@FileType(mime = "image")
@Constraint(validatedBy = [])
@Target(
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.ANNOTATION_CLASS
)
annotation class Image(
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

@NotBlank(message = "EMPTY")
@Size(max = MAX_NAME_LENGTH, message = "LARGE")
@Constraint(validatedBy = [])
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER
)
annotation class Nick(
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

@NotBlankNullable(message = "EMPTY")
@Size(max = MAX_NAME_LENGTH, message = "LARGE")
@Constraint(validatedBy = [])
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER
)
annotation class NickNullable(
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

@NotBlankNullable(message = "EMPTY")
@Size(max = MAX_NAME_LENGTH, message = "LARGE")
@Constraint(validatedBy = [])
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER
)
annotation class Name(
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

@NotBlank(message = "EMPTY")
@Email(message = "PATTERN")
@Size(max = MAX_USERNAME_LENGTH, message = "LARGE")
@Constraint(validatedBy = [])
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER
)
annotation class UserName(
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

@NotBlank(message = "EMPTY")
@Pattern(regexp = PASSWORD_REGEX, message = "PATTERN")
@Constraint(validatedBy = [])
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER
)
annotation class Password(
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

@NotBlank(message = "EMPTY")
@Size(max = MAX_TITLE_LENGTH, message = "LARGE")
@Constraint(validatedBy = [])
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER
)
annotation class Title(
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

@NotBlankNullable(message = "EMPTY")
@Size(max = MAX_TITLE_LENGTH, message = "LARGE")
@Constraint(validatedBy = [])
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER
)
annotation class TitleNullable(
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

@NotBlankNullable(message = "EMPTY")
@Size(max = MAX_TITLE_LENGTH, message = "LARGE")
@Constraint(validatedBy = [])
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER
)
annotation class Alias(
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

@NotBlankNullable(message = "EMPTY")
@Size(max = MAX_DESCRIPTION_LENGTH, message = "LARGE")
@Constraint(validatedBy = [])
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER
)
annotation class Description(
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

@NotNull(message = "EMPTY")
@Positive(message = "LOW")
@Max(value = Long.MAX_VALUE, message = "HIGH")
@Constraint(validatedBy = [])
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER
)
annotation class LongId(
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

@Positive(message = "LOW")
@Max(value = Long.MAX_VALUE, message = "HIGH")
@Constraint(validatedBy = [])
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER
)
annotation class LongIdNullable(
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

@PositiveOrZero(message = "LOW")
@Max(value = Long.MAX_VALUE, message = "HIGH")
@Constraint(validatedBy = [])
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER
)
annotation class PartIndex(
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

@Positive(message = "LOW")
@Max(value = 100, message = "HIGH")
@Constraint(validatedBy = [])
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER
)
annotation class PartSize(
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

@NotNull(message = "EMPTY")
@Pattern(
    regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$",
    message = "PATTERN"
)
@Constraint(validatedBy = [])
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER
)
annotation class ValidUUID(
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

@Constraint(validatedBy = [NotBlankNullableValidator::class])
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER
)
annotation class NotBlankNullable(
    val message: String = "EMPTY",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class NotBlankNullableValidator : ConstraintValidator<NotBlankNullable, String?> {
    override fun isValid(p0: String?, p1: ConstraintValidatorContext?): Boolean {
        return p0 == null || p0.isNotBlank()
    }
}

@NotNull(message = "EMPTY")
@Size(max = 4, message = "LONG")
@Pattern(regexp = "^[A-Za-z0-9]{4}$", message = "PATTERN")
@Constraint(validatedBy = [])
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER
)
annotation class Code(
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

sealed interface FileValidator {
    fun validate(file: File): Result<Unit, FileError>
    interface ImageValidator : FileValidator
    interface VideoValidator : FileValidator
}

@Component
class Jave2VideoValidator : FileValidator.VideoValidator {
    private companion object {
        val allowedExtensions = mapOf(
            "mp4" to listOf("mp4", "m4v", "mov"),
            "mov" to listOf("mov", "mp4", "m4v"),
            "mkv" to listOf("mkv", "mk3d", "mka"),
            "webm" to listOf("webm"),
            "avi" to listOf("avi"),
            "flv" to listOf("flv"),
            "3gp" to listOf("3gp"),
            "mpeg" to listOf("mpeg", "mpg")
        )
    }
    override fun validate(file: File): Result<Unit, FileError> {
        return try {
            val media = MultimediaObject(file)
            val info = media.info
            val extension = file.extension.lowercase()
            if (
                info?.video != null
                && info.video.size.width > 0
                && info.video.size.height > 0
                && info.video.frameRate * (info.duration / 1000f) >= 1
                && allowedExtensions[info.format.lowercase()]?.contains(extension) == true
            ) {
                Result.Success(Unit)
            } else {
                Result.Failure(FileError.NOT_VALID)
            }
        } catch (_: Exception) {
            Result.Failure(FileError.NOT_VALID)
        }
    }
}

@Component
class ImagingValidator : FileValidator.ImageValidator {
    private companion object {
        val allowedExtensions = mapOf(
            "jpeg" to listOf("jpg", "jpeg"),
            "png" to listOf("png"),
            "gif" to listOf("gif"),
            "bmp" to listOf("bmp"),
            "tiff" to listOf("tif", "tiff"),
            "webp" to listOf("webp"),
            "avif" to listOf("avif")
        )
    }
    override fun validate(file: File): Result<Unit, FileError> {
        return try {
            val extension = file.extension.lowercase()
            val info = Imaging.getImageInfo(file)
            if (
                info != null
                && info.width > 0
                && info.height > 0
                && allowedExtensions[info.format?.name?.lowercase()]?.contains(extension) == true
            ) {
                Result.Success(Unit)
            } else {
                Result.Failure(FileError.NOT_VALID)
            }
        } catch (_: Exception) {
            Result.Failure(FileError.NOT_VALID)
        }
    }
}

interface VideoMetaDataExtractor {
    fun extract(file: File): VideoMetaData?
}

@Component
class StandardVideoMetaDataExtractor : VideoMetaDataExtractor {
    override fun extract(file: File): VideoMetaData? {
        return VideoMetaData(
            fileName = file.name,
            mimeType = MediaTypeFactory.getMediaType(file.name)
                .orElse(MediaType.valueOf("application/octet-stream"))
                .toString(),
            size = file.length()
        )
    }
}