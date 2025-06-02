package mikhail.shell.video.hosting.controllers

import mikhail.shell.video.hosting.domain.AuthModel
import mikhail.shell.video.hosting.dto.SignUpDto
import mikhail.shell.video.hosting.dto.toDomain
import mikhail.shell.video.hosting.errors.CompoundError
import mikhail.shell.video.hosting.errors.HostingDataException
import mikhail.shell.video.hosting.errors.SignInError
import mikhail.shell.video.hosting.errors.SignUpError
import mikhail.shell.video.hosting.service.AuthService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthenticationController(
    private val authService: AuthService
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
            throw HostingDataException(compoundError)
        }
        return authService.signInWithPassword(username, password)
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
            throw HostingDataException(compoundError)
        }
        return authService.signUpWithPassword(
            signUpDto.userName,
            signUpDto.password,
            signUpDto.userDto.toDomain()
        )
    }
}