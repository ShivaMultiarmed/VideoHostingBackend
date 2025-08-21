package mikhail.shell.video.hosting.domain

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import org.springframework.web.multipart.MultipartFile
import kotlin.reflect.KClass

object ValidationRules {
    const val CODE_LENGTH = 4
    const val MAX_TITLE_LENGTH = 100
    const val MAX_NAME_LENGTH = 50
    const val MAX_TEXT_LENGTH = 255
    const val MAX_USERNAME_LENGTH = 50
    const val MAX_IMAGE_SIZE = 10 * 1024 * 1024
    const val MAX_VIDEO_SIZE = 512 * 1024 * 1024
    val PASSWORD_REGEX = Regex("^(?=.*[0-9])(?=.*[^a-zA-Z0-9])\\S{8,20}$")
    val EMAIL_REGEX = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$")
}

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [FileSizeValidator::class])
annotation class FileSize(
    val max: Long,
    val mime: String,
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class FileSizeValidator: ConstraintValidator<FileSize, MultipartFile?> {

    private var max: Long = 0
    private var mime: String = ""

    override fun initialize(constraintAnnotation: FileSize?) {
        max = constraintAnnotation?.max?: max
        mime = constraintAnnotation?.mime?: mime
    }

    override fun isValid(p0: MultipartFile?, p1: ConstraintValidatorContext?): Boolean {
        return p0!= null && !p0.isEmpty && p0.size <= max && p0.contentType?.contains(mime)?: false
    }
}
