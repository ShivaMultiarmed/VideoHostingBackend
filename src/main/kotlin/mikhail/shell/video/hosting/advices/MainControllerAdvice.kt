package mikhail.shell.video.hosting.advices

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import mikhail.shell.video.hosting.dto.camelToSnakeCase
import mikhail.shell.video.hosting.errors.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingPathVariableException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.support.MissingServletRequestPartException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class MainControllerAdvice {
    @ExceptionHandler(ValidationException::class)
    fun handleValidationException(e: ValidationException): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            e.errors.map {
                val key = ("${it.key}Error").camelToSnakeCase()
                val value = if (it.value is Enum<*>) (it.value as Enum<*>).name.lowercase() else it.toString()
                key to value
            }.toMap()
        )
    }

    @ExceptionHandler(
        NoSuchElementException::class,
        NoResourceFoundException::class
    )
    fun handleNotFoundException(e: Exception): ResponseEntity<Unit> {
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
    fun handleUnauthenticatedAccess(e: UnauthenticatedException): ResponseEntity<Unit> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
    }

    @ExceptionHandler(UniquenessViolationException::class)
    fun handleConflict(e: UniquenessViolationException): ResponseEntity<Unit> {
        return ResponseEntity.status(HttpStatus.CONFLICT).build()
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(e: MethodArgumentNotValidException): ResponseEntity<Map<String, String>> {
        return ResponseEntity.badRequest().body(
            e.bindingResult.allErrors.associate { error ->
                val key = ((error as FieldError).field.substringAfterLast(".") + "Error").camelToSnakeCase()
                val value = (error.defaultMessage ?: UnexpectedError.toString()).lowercase()
                key to value
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
                                    this[(error.field.substringAfterLast(".") + "Error").camelToSnakeCase()] = (error.defaultMessage ?: UnexpectedError.toString()).lowercase()
                                }
                                else -> {
                                    val parameterName = validationResult.methodParameter.parameterName
                                    if (parameterName != null && error.defaultMessage != null) {
                                        this[(parameterName + "Error").camelToSnakeCase()] = error.defaultMessage!!.lowercase()
                                    }
                                }
                            }
                        }
                    }
                }
            )
    }
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun malformedRequestBodyHandler(e: HttpMessageNotReadableException): ResponseEntity<*> {
        val cause = e.cause
        val key: String
        val value: String
        when (cause) {
            null -> {
                key = "valueError".camelToSnakeCase()
                value = "EMPTY".lowercase()
            }
            is InvalidFormatException -> {
                val fieldName = cause.path.lastOrNull()?.fieldName ?: "value"
                key = (fieldName + "Error").camelToSnakeCase()
                value = "NOT_VALID".lowercase()
            }
            else -> {
                key = "valueError".camelToSnakeCase()
                value = "NOT_VALID".lowercase()
            }
        }
        val response = mapOf(key to value)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun malformedRequestParamsHandler(e: MethodArgumentTypeMismatchException): ResponseEntity<*> {
        val paramName = e.name
        val key = "${paramName}Error".camelToSnakeCase()
        val value = "NOT_VALID".lowercase()
        val response = mutableMapOf(key to value)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }
    @ExceptionHandler(
        MissingPathVariableException::class,
        MissingServletRequestParameterException::class,
        MissingServletRequestPartException::class,
        MissingRequestHeaderException::class
    )
    fun absentRequestPartHandler(e: Exception): ResponseEntity<*> {
        val partName = when (e) {
            is MissingPathVariableException -> e.variableName
            is MissingServletRequestParameterException -> e.parameterName
            is MissingServletRequestPartException -> e.requestPartName
            is MissingRequestHeaderException -> e.headerName
            else -> "requestPart"
        }
        val key = "${partName}Error".camelToSnakeCase()
        val value = "EMPTY".lowercase()
        val response = mutableMapOf(key to value)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun methodTypeErrorHandler(e: HttpRequestMethodNotSupportedException): ResponseEntity<*> {
        val key = "supportedMethods".camelToSnakeCase()
        val value = e.supportedMethods?.map { it.lowercase() }
        val response = mapOf(key to value)
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response)
    }
    @ExceptionHandler(Exception::class)
    fun exceptionHandler(e: Exception): ResponseEntity<*> {
        e.printStackTrace()
        val key = "error".camelToSnakeCase()
        val value = UnexpectedError.toString().lowercase()
        val response = mapOf(key to value)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
    }
}