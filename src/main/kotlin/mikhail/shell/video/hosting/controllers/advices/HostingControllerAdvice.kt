package mikhail.shell.video.hosting.controllers.advices

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import jakarta.validation.ConstraintViolationException
import mikhail.shell.video.hosting.errors.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindException
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException

@RestControllerAdvice
class HostingControllerAdvice {
    @ExceptionHandler(ValidationException::class)
    fun handleValidationException(e: ValidationException): ResponseEntity<Map<String, Error>> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.errors)
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFoundException(e: NoSuchElementException): ResponseEntity<Unit> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleNotCorrectUserData(e: IllegalArgumentException): ResponseEntity<Unit> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
    }

    @ExceptionHandler(IllegalAccessException::class)
    fun handleIllegalAccess(e: IllegalAccessException): ResponseEntity<Unit> {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    }

    @ExceptionHandler(UnauthenticatedException::class)
    fun handleIllegalAccess(e: UnauthenticatedException): ResponseEntity<Unit> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
    }

    @ExceptionHandler(UniquenessViolationException::class)
    fun handleIllegalAccess(e: UniquenessViolationException): ResponseEntity<Unit> {
        return ResponseEntity.status(HttpStatus.CONFLICT).build()
    }

    @ExceptionHandler(ExpiredException::class)
    fun handleExpiration(e: ExpiredException): ResponseEntity<Unit> {
        return ResponseEntity.status(HttpStatus.GONE).build()
    }

//    @ExceptionHandler(BindException::class)
//    fun handleNotValidArguments(e: BindException): ResponseEntity<Map<String, String>> {
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
//            e.bindingResult.allErrors.associate {
//                (it as FieldError).field to (it.defaultMessage ?: UnexpectedError.toString())
//            }
//        )
//    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(e: MethodArgumentNotValidException): ResponseEntity<Map<String, String>> {
        return ResponseEntity.badRequest().body(
            e.bindingResult.allErrors.associate { error ->
                (error as FieldError).field to (error.defaultMessage ?: UnexpectedError.toString())
            }
        )
    }

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleNotValidParts(e: HandlerMethodValidationException): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                mutableMapOf<String, String>().apply {
                    e.parameterValidationResults.forEach { validationResult ->
                        validationResult.resolvableErrors.forEach { error ->
                            when (error) {
                                is FieldError -> {
                                    this[error.field] = error.defaultMessage ?: UnexpectedError.toString()
                                }
                                else -> {
                                    val parameterName = validationResult.methodParameter.parameterName
                                    if (parameterName != null && error.defaultMessage != null) {
                                        this[parameterName] = error.defaultMessage!!
                                    }
                                }
                            }
                        }
                    }
                }
            )
    }
//
//    @ExceptionHandler(ConstraintViolationException::class)
//    fun handleConstraintViolationException(e: ConstraintViolationException): ResponseEntity<Map<String, String>> {
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
//            e.constraintViolations.associate { violation ->
//                violation.propertyPath.last().name to violation.message
//            }
//        )
//    }

    @ExceptionHandler(Exception::class)
    fun handleExceptions(e: Exception): ResponseEntity<Unit> {
        e.printStackTrace()
        return ResponseEntity.internalServerError().build()
    }
}
