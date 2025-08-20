package mikhail.shell.video.hosting.controllers

import jakarta.servlet.http.HttpServletRequest
import mikhail.shell.video.hosting.domain.AuthModel
import mikhail.shell.video.hosting.domain.ValidationRules
import mikhail.shell.video.hosting.domain.ValidationRules.EMAIL_REGEX
import mikhail.shell.video.hosting.dto.SignUpDto
import mikhail.shell.video.hosting.dto.toDomain
import mikhail.shell.video.hosting.errors.*
import mikhail.shell.video.hosting.service.AuthService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/signin/password")
    fun signInWithPassword(
        @RequestParam username: String,
        @RequestParam password: String
    ): AuthModel {
        val compoundError = CompoundError<SignInError>()
        if (username.isEmpty()) {
            compoundError.add(SignInError.USERNAME_EMPTY)
        } else if (!username.matches(EMAIL_REGEX)) {
            compoundError.add(SignInError.USERNAME_MALFORMED)
        }
        if (password.isEmpty()) {
            compoundError.add(SignInError.PASSWORD_EMPTY)
        }
        if (compoundError.isNotEmpty()) {
            throw ValidationException(compoundError)
        }
        return authService.signInWithPassword(username, password)
    }

    @PostMapping("/signup/password/request")
    fun requestSignUpWithPassword(@RequestParam userName: String) {
        val compoundError = CompoundError<SignUpError>()
        if (userName.isEmpty()) {
            compoundError.add(SignUpError.USERNAME_EMPTY)
        } else if (!userName.matches(EMAIL_REGEX)) {
            compoundError.add(SignUpError.USERNAME_MALFORMED)
        }
        if (compoundError.isNotEmpty()) {
            throw ValidationException(compoundError)
        }
        authService.requestSignUpWithPassword(userName)
    }

    @PostMapping("/signup/password/verify")
    fun verifySignUpWithPassword(
        @RequestParam userName: String,
        @RequestParam code: String
    ): String {
        val compoundError = CompoundError<SignUpError>()
        if (userName.isEmpty()) {
            compoundError.add(SignUpError.USERNAME_EMPTY)
        } else if (!userName.matches(EMAIL_REGEX)) {
            compoundError.add(SignUpError.USERNAME_MALFORMED)
        }
        if (compoundError.isNotEmpty()) {
            throw ValidationException(compoundError)
        }
        return authService.verifySignUpWithPassword(userName, code)
    }

    @PostMapping("/signup/password/confirm")
    fun confirmSignUpWithPassword(
        request: HttpServletRequest,
        @RequestBody signUpDto: SignUpDto
    ) {
        val compoundError = CompoundError<SignUpError>()
        val token = request.getHeader(HttpHeaders.AUTHORIZATION).removePrefix("Bearer ")
        if (signUpDto.password.isEmpty()) {
            compoundError.add(SignUpError.PASSWORD_EMPTY)
        }
        if (signUpDto.userDto.nick.isEmpty()) {
            compoundError.add(SignUpError.NICK_EMPTY)
        }
        if (compoundError.isNotEmpty()) {
            throw ValidationException(compoundError)
        }
        authService.confirmSignUpWithPassword(
            token = token,
            password = signUpDto.password,
            user = signUpDto.userDto.toDomain()
        )
    }

    @PostMapping("/reset/password/request")
    fun requestPasswordReset(@RequestParam userName: String) {
        if (userName.isEmpty()) {
            throw ValidationException(ResetError.USERNAME_EMPTY)
        } else {
            authService.requestPasswordReset(userName)
        }
    }

    @PostMapping("/reset/password/verify")
    fun verifyPasswordReset(
        @RequestParam userName: String,
        @RequestParam code: String
    ) = authService.verifyPasswordReset(
        userName = userName,
        code = code
    )

    @PostMapping("/reset/password/confirm")
    fun confirmPasswordReset(
        request: HttpServletRequest,
        @RequestParam password: String
    ): ResponseEntity<Unit> {
        val resetToken = request.getHeader("Authorization")?.removePrefix("Bearer ")
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        if (password.isEmpty()) {
            throw ValidationException(ResetError.PASSWORD_EMPTY)
        }
        authService.resetPassword(resetToken, password)
        return ResponseEntity.status(HttpStatus.OK).build()
    }

    @PostMapping("/signout")
    fun signOut(request: HttpServletRequest): ResponseEntity<Unit> {
        val token = request.getHeader("Authorization")?.substring("Bearer ".length)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        authService.signOut(token)
        return ResponseEntity.ok().build()
    }
}