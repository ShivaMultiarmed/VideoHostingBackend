package mikhail.shell.video.hosting.controllers.advices

import mikhail.shell.video.hosting.errors.Error
import mikhail.shell.video.hosting.errors.ValidationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class HostingControllerAdvice {
    @ExceptionHandler(ValidationException::class)
    fun handleDataException(e: ValidationException): ResponseEntity<Error> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.error)
    }
    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFoundException(e: NoSuchElementException): ResponseEntity<Unit> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
    }
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleNotCorrectUserData(): ResponseEntity<Unit> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
    }
}