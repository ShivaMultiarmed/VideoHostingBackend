package mikhail.shell.video.hosting.controllers

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import mikhail.shell.video.hosting.domain.AuthModel
import mikhail.shell.video.hosting.domain.ValidationRules
import mikhail.shell.video.hosting.domain.ValidationRules.EMAIL_REGEX
import mikhail.shell.video.hosting.dto.SignUpDto
import mikhail.shell.video.hosting.dto.toDomain
import mikhail.shell.video.hosting.errors.*
import mikhail.shell.video.hosting.service.AuthService
import org.hibernate.validator.constraints.Length
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
        @RequestParam @NotBlank @Email @Max(ValidationRules.MAX_USERNAME_LENGTH.toLong()) username: String,
        @RequestParam @NotBlank @Pattern(regexp = "^(?=.*[0-9])(?=.*[^a-zA-Z0-9])\\S{8,20}$") password: String
    ): AuthModel {
        return authService.signInWithPassword(username, password)
    }

    @PostMapping("/signup/password/request")
    fun requestSignUpWithPassword(@RequestParam userName: String) {
        authService.requestSignUpWithPassword(userName)
    }

    @PostMapping("/signup/password/verification")
    fun verifySignUpWithPassword(
        @RequestParam userName: String,
        @RequestParam code: String
    ): String {
        return authService.verifySignUpWithPassword(userName, code)
    }

    @PostMapping("/signup/password/confirmation")
    fun confirmSignUpWithPassword(
        request: HttpServletRequest,
        @RequestBody signUpDto: SignUpDto
    ) {
        authService.confirmSignUpWithPassword(
            token = request.getHeader(HttpHeaders.AUTHORIZATION).removePrefix("Bearer "),
            password = signUpDto.password,
            user = signUpDto.userDto.toDomain()
        )
    }

    @PostMapping("/reset/password/request")
    fun requestPasswordReset(@RequestParam @NotBlank @Email userName: String) {
        authService.requestPasswordReset(userName)
    }

    @PostMapping("/reset/password/verify")
    fun verifyPasswordReset(
        @RequestParam @NotBlank @Email userName: String,
        @RequestParam @NotBlank @Length(min = 4 , max = 4) code: String
    ) = authService.verifyPasswordReset(
        userName = userName,
        code = code
    )

    @PostMapping("/reset/password/confirm")
    fun confirmPasswordReset(
        request: HttpServletRequest,
        @RequestParam @NotBlank @Pattern(regexp = ValidationRules.PASSWORD_REGEX) password: String
    ) {
        val resetToken = request.getHeader("Authorization")?.removePrefix("Bearer ")
            ?: throw UnauthenticatedException()
        authService.resetPassword(resetToken, password)
    }

    @PostMapping("/signout")
    fun signOut(request: HttpServletRequest) {
        val token = request.getHeader("Authorization")?.substring("Bearer ".length)
            ?: throw UnauthenticatedException()
        authService.signOut(token)
    }
}