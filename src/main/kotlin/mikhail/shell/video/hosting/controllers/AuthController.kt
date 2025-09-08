package mikhail.shell.video.hosting.controllers

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import mikhail.shell.video.hosting.domain.AuthModel
import mikhail.shell.video.hosting.domain.User
import mikhail.shell.video.hosting.domain.ValidationRules
import mikhail.shell.video.hosting.domain.ValidationRules.MAX_USERNAME_LENGTH
import mikhail.shell.video.hosting.domain.ValidationRules.PASSWORD_REGEX
import mikhail.shell.video.hosting.dto.SignUpRequest
import mikhail.shell.video.hosting.errors.*
import mikhail.shell.video.hosting.service.AuthService
import mikhail.shell.video.hosting.service.UserService
import org.hibernate.validator.constraints.Length
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v2/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/signin/password")
    fun signInWithPassword(
        @RequestParam @Email(message = "PATTERN") @Size(max = MAX_USERNAME_LENGTH, message = "LARGE") username: String,
        @RequestParam @NotBlank(message = "EMPTY") @Pattern(regexp = PASSWORD_REGEX, message = "PATTERN") password: String
    ) = authService.signInWithPassword(username, password)

    @PostMapping("/signup/password/request")
    fun requestSignUpWithPassword(@RequestParam @Email(message = "PATTERN") @Size(max = MAX_USERNAME_LENGTH, message = "LARGE") userName: String) {
        authService.requestSignUpWithPassword(userName)
    }

    @PostMapping("/signup/password/verification")
    fun verifySignUpWithPassword(
        @RequestParam @Email(message = "PATTERN") @Size(max = MAX_USERNAME_LENGTH, message = "LARGE") userName: String,
        @RequestParam @Size(min = 4, max = 4, message = "NOT_CORRECT") code: String
    ) = authService.verifySignUpWithPassword(userName = userName, code = code)

    @PostMapping("/signup/password/confirmation")
    fun confirmSignUpWithPassword(
        request: HttpServletRequest,
        @RequestBody @Valid signUpRequest: SignUpRequest
    ) {
        authService.confirmSignUpWithPassword(
            token = request.getHeader(HttpHeaders.AUTHORIZATION).removePrefix("Bearer "),
            password = signUpRequest.password,
            user = User(
                nick = signUpRequest.userCreatingRequest.nick,
                name = signUpRequest.userCreatingRequest.name,
                bio = signUpRequest.userCreatingRequest.bio,
                tel = signUpRequest.userCreatingRequest.tel,
                email = signUpRequest.userCreatingRequest.nick,
            )
        )
    }

    @PostMapping("/reset/password/request")
    fun requestPasswordReset(@RequestParam @Email(message = "PATTERN") userName: String) = authService.requestPasswordReset(userName)

    @PostMapping("/reset/password/verification")
    fun verifyPasswordReset(
        @RequestParam @Email(message = "PATTERN") userName: String,
        @RequestParam @Size(min = 4, max = 4, message = "NOT_CORRECT") code: String
    ) = authService.verifyPasswordReset(
        userName = userName,
        code = code
    )

    @PostMapping("/reset/password/confirmation")
    fun confirmPasswordReset(
        request: HttpServletRequest,
        @RequestParam @NotBlank(message = "EMPTY") @Pattern(regexp = PASSWORD_REGEX) password: String
    ) {
        val resetToken = request.getHeader("Authorization")?.removePrefix("Bearer ")
            ?: throw UnauthenticatedException()
        authService.resetPassword(resetToken, password)
    }

    @PostMapping("/signout")
    fun signOut(request: HttpServletRequest) {
        val token = request.getHeader("Authorization")?.removePrefix("Bearer ")
            ?: throw UnauthenticatedException()
        authService.signOut(token)
    }

    @GetMapping("/existence")
    fun existsById(@RequestParam @Email(message = "PATTERN") userName: String) = authService.existsByUserName(userName)
}