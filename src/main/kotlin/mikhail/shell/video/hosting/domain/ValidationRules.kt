package mikhail.shell.video.hosting.domain

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import mikhail.shell.video.hosting.domain.ValidationRules.MAX_DESCRIPTION_LENGTH
import mikhail.shell.video.hosting.domain.ValidationRules.MAX_IMAGE_SIZE
import mikhail.shell.video.hosting.domain.ValidationRules.MAX_NAME_LENGTH
import mikhail.shell.video.hosting.domain.ValidationRules.MAX_TITLE_LENGTH
import org.springframework.web.multipart.MultipartFile
import kotlin.reflect.KClass

object ValidationRules {
    const val CODE_LENGTH = 4
    const val MAX_TITLE_LENGTH = 100
    const val MAX_NAME_LENGTH = 50
    const val MAX_TEXT_LENGTH = 255
    const val MAX_USERNAME_LENGTH = 50
    const val MAX_DESCRIPTION_LENGTH = 255
    const val MAX_IMAGE_SIZE = 10 * 1024 * 1024L
    const val MAX_VIDEO_SIZE = 512 * 1024 * 1024
    const val MIN_PASSWORD_LENGTH = 8
    const val MAX_PASSWORD_LENGTH = 20
    const val PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[^a-zA-Z0-9])\\S{$MIN_PASSWORD_LENGTH,$MAX_PASSWORD_LENGTH}$"
    const val EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$"
    const val TEL_REGEX = "^\\d{8,15}\$"
}

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [MaxFileSizeValidator::class])
annotation class MaxFileSize(
    val max: Long,
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class MaxFileSizeValidator: ConstraintValidator<MaxFileSize, MultipartFile?> {
    private var max: Long = 0

    override fun initialize(constraintAnnotation: MaxFileSize?) {
        max = constraintAnnotation?.max?: max
    }

    override fun isValid(p0: MultipartFile?, p1: ConstraintValidatorContext?): Boolean {
        return p0!= null && !p0.isEmpty && p0.size <= max
    }
}

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [NotEmptyFileValidator::class])
annotation class NotEmptyFile(
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class NotEmptyFileValidator: ConstraintValidator<MaxFileSize, MultipartFile?> {

    override fun isValid(p0: MultipartFile?, p1: ConstraintValidatorContext?): Boolean {
        return p0!= null && !p0.isEmpty
    }
}

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [FileTypeValidator::class])
annotation class FileType(
    val mime: String,
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class FileTypeValidator: ConstraintValidator<FileType, MultipartFile?> {
    private var mime: String = ""

    override fun initialize(constraintAnnotation: FileType?) {
        mime = constraintAnnotation?.mime?: mime
    }

    override fun isValid(p0: MultipartFile?, p1: ConstraintValidatorContext?): Boolean {
        return p0!= null && !p0.isEmpty && p0.contentType?.startsWith(mime)?: false
    }
}

@NotBlank(message = "EMPTY")
@Size(max = MAX_NAME_LENGTH, message = "LARGE")
@Constraint(validatedBy = [])
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.PROPERTY_GETTER
)
annotation class Name(
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

@NotBlank(message = "EMPTY")
@Size(max = MAX_TITLE_LENGTH, message = "LARGE")
@Constraint(validatedBy = [])
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.PROPERTY_GETTER
)
annotation class Title(
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

@NotBlank(message = "EMPTY")
@Size(max = MAX_DESCRIPTION_LENGTH, message = "LARGE")
@Constraint(validatedBy = [])
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.PROPERTY_GETTER
)
annotation class Description(
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

@Positive(message = "LOW")
@Max(value = Long.MAX_VALUE, message = "HIGH")
@Constraint(validatedBy = [])
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.PROPERTY_GETTER
)
annotation class LongId(
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

@NotEmptyFile(message = "EMPTY")
@MaxFileSize(max = MAX_IMAGE_SIZE, message = "LARGE")
@FileType(mime = "image", message = "NOT_SUPPORTED")
@Constraint(validatedBy = [])
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.PROPERTY_GETTER
)
annotation class Image(
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)