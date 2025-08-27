package mikhail.shell.video.hosting.controllers.advices

import mikhail.shell.video.hosting.errors.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class HostingControllerAdvice {
    @ExceptionHandler(ValidationException::class)
    fun handleValidationException(e: ValidationException): ResponseEntity<Error> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.error)
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
}