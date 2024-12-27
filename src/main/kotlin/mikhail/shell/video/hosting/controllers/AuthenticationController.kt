package mikhail.shell.video.hosting.controllers

import mikhail.shell.video.hosting.domain.AuthModel
import mikhail.shell.video.hosting.dto.SignUpDto
import mikhail.shell.video.hosting.dto.toDomain
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
    @PostMapping("/signin/password")
    fun signInWithPassword(
        @RequestParam username: String?,
        @RequestParam password: String?
    ): AuthModel {
        return authService.signInWithPassword(username, password)
    }
    @PostMapping("/signup/password")
    fun signUpWithPassword(
        @RequestBody signUpDto: SignUpDto
    ): AuthModel {
        return authService.signUpWithPassword(
            signUpDto.userName,
            signUpDto.password,
            signUpDto.userDto.toDomain()
        )
    }
}