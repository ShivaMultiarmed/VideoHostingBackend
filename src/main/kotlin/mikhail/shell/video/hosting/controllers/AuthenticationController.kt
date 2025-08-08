package mikhail.shell.video.hosting.controllers

import jakarta.servlet.http.HttpServletRequest
import mikhail.shell.video.hosting.domain.AuthModel
import mikhail.shell.video.hosting.dto.SignUpDto
import mikhail.shell.video.hosting.dto.toDomain
import mikhail.shell.video.hosting.errors.CompoundError
import mikhail.shell.video.hosting.errors.ValidationException
import mikhail.shell.video.hosting.errors.SignInError
import mikhail.shell.video.hosting.errors.SignUpError
import mikhail.shell.video.hosting.service.AuthenticationService
import mikhail.shell.video.hosting.service.AuthenticationServiceWithDB
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthenticationController(
    private val authenticationService: AuthenticationService
) {
    val emailRegex = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$")

    @PostMapping("/signin/password")
    fun signInWithPassword(
        @RequestParam username: String,
        @RequestParam password: String
    ): AuthModel {
        val compoundError = CompoundError<SignInError>()
        if (username.isEmpty()) {
            compoundError.add(SignInError.USERNAME_EMPTY)
        } else if (!username.matches(emailRegex)) {
            compoundError.add(SignInError.USERNAME_MALFORMED)
        }
        if (password.isEmpty()) {
            compoundError.add(SignInError.PASSWORD_EMPTY)
        }
        if (compoundError.isNotNull()) {
            throw ValidationException(compoundError)
        }
        return authenticationService.signInWithPassword(username, password)
    }

    @PostMapping("/signup/password")
    fun signUpWithPassword(
        @RequestBody signUpDto: SignUpDto
    ): AuthModel {
        val compoundError = CompoundError<SignUpError>()
        if (signUpDto.userName.isEmpty()) {
            compoundError.add(SignUpError.USERNAME_EMPTY)
        } else if (!signUpDto.userName.matches(emailRegex)) {
            compoundError.add(SignUpError.USERNAME_MALFORMED)
        }
        if (signUpDto.password.isEmpty()) {
            compoundError.add(SignUpError.PASSWORD_EMPTY)
        }
        if (signUpDto.userDto.nick.isEmpty()) {
            compoundError.add(SignUpError.NICK_EMPTY)
        }
        if (compoundError.isNotNull()) {
            throw ValidationException(compoundError)
        }
        return authenticationService.signUpWithPassword(
            signUpDto.userName,
            signUpDto.password,
            signUpDto.userDto.toDomain()
        )
    }

    @PostMapping("/reset/password/request")
    fun requestPasswordReset(@RequestParam userName: String) = authenticationService.requestPasswordReset(userName)

    @PostMapping("/reset/password/verify")
    fun verifyPasswordReset(
        @RequestParam userId: Long,
        @RequestParam resetCode: String
    ) = authenticationService.verifyPasswordReset(
        userId = userId,
        resetCode = resetCode
    )

    @PostMapping("/reset/password/confirm")
    fun confirmPasswordReset(
        request: HttpServletRequest,
        @RequestParam password: String
    ) {
        val resetToken = request.getHeader("Authorization")?.substring("Bearer ".length)?: throw IllegalAccessException()
        if (password.isEmpty()) {
            throw IllegalArgumentException()
        }
        return authenticationService.resetPassword(resetToken, password)
    }
}