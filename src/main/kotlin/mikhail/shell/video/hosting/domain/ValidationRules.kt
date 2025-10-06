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
import mikhail.shell.video.hosting.errors.TextError
import org.springframework.web.multipart.MultipartFile
import kotlin.reflect.KClass

object ValidationRules {
    const val FILE_NAME_REGEX = "^\\S+\\.\\S+$"
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
class MaxFileSizeValidator: ConstraintValidator<MaxFileSize, MultipartFile?> {
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
class NotEmptyFileValidator: ConstraintValidator<NotEmptyFile, MultipartFile?> {
    override fun isValid(p0: MultipartFile?, p1: ConstraintValidatorContext): Boolean {
        return p0 == null || !p0.isEmpty
    }
}

@Target(
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.ANNOTATION_CLASS
)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [FileNameValidator::class])
annotation class FileName(
    val message: String = "NAME_NOT_VALID",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
class FileNameValidator: ConstraintValidator<FileName, MultipartFile?> {
    override fun isValid(p0: MultipartFile?, p1: ConstraintValidatorContext): Boolean {
        return p0 == null || p0.originalFilename!!.length <= MAX_TITLE_LENGTH && p0.originalFilename!!.matches(FILE_NAME_REGEX.toRegex())
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
class FileTypeValidator: ConstraintValidator<FileType, MultipartFile?> {
    private var mime: String = ""
    override fun initialize(constraintAnnotation: FileType) {
        mime = constraintAnnotation.mime
    }
    override fun isValid(p0: MultipartFile?, p1: ConstraintValidatorContext?): Boolean {
        return p0 == null || !p0.isEmpty && p0.contentType?.startsWith(mime)?: false
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
class NotBlankNullableValidator: ConstraintValidator<NotBlankNullable, String?> {
    override fun isValid(p0: String?, p1: ConstraintValidatorContext?): Boolean {
        return p0 == null || p0.isNotBlank()
    }
}

@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER
)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [EnumValidator::class])
annotation class ValidEnum(
    val enumClass: KClass<out Enum<*>>,
    val message: String = "PATTERN",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class EnumValidator : ConstraintValidator<ValidEnum, String?> {
    private lateinit var acceptedValues: Set<String>

    override fun initialize(annotation: ValidEnum) {
        acceptedValues = annotation.enumClass.java.enumConstants.map { it.name }.toSet()
    }

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        return acceptedValues.contains(value?.uppercase())
    }
}